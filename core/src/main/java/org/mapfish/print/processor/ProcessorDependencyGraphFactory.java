package org.mapfish.print.processor;

import static org.mapfish.print.parser.ParserUtils.getAllAttributes;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.BiMap;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections.CollectionUtils;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.HttpRequestHeadersAttribute;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.ParserUtils;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class for constructing {@link org.mapfish.print.processor.ProcessorDependencyGraph} instances.
 */
public final class ProcessorDependencyGraphFactory {

  @Autowired private MetricRegistry metricRegistry;

  private static Set<InputValue> getInputs(final Processor<?, ?> processor) {
    final BiMap<String, String> inputMapper = processor.getInputMapperBiMap();
    final Set<InputValue> inputs = new HashSet<>();

    final Object inputParameter = processor.createInputParameter();
    if (inputParameter != null) {
      verifyAllMappingsMatchParameter(
          inputMapper.values(),
          inputParameter.getClass(),
          "One or more of the input mapping values of '"
              + processor
              + "'  do not match an input"
              + " parameter.  The bad mappings are");

      final Collection<Field> allProperties = getAllAttributes(inputParameter.getClass());
      for (Field field : allProperties) {
        String name =
            ProcessorUtils.getInputValueName(
                processor.getInputPrefix(), inputMapper, field.getName());
        inputs.add(new InputValue(name, field));
      }
    }

    return inputs;
  }

  private static Collection<OutputValue> getOutputValues(final Processor<?, ?> processor) {
    final Map<String, String> outputMapper = processor.getOutputMapperBiMap();
    final Set<OutputValue> values = new HashSet<>();

    final Set<String> mappings = outputMapper.keySet();
    final Class<?> paramType = processor.getOutputType();
    verifyAllMappingsMatchParameter(
        mappings,
        paramType,
        "One or more of the output mapping keys of '"
            + processor
            + "' do not match an output parameter.  The bad mappings are: ");
    final Collection<Field> allProperties = getAllAttributes(paramType);
    for (Field field : allProperties) {
      // if the field is annotated with @DebugValue, it can be renamed automatically in a
      // mapping in case of a conflict.
      final boolean canBeRenamed = field.getAnnotation(InternalValue.class) != null;
      String name =
          ProcessorUtils.getOutputValueName(processor.getOutputPrefix(), outputMapper, field);
      values.add(new OutputValue(name, canBeRenamed, field));
    }

    return values;
  }

  /**
   * Fill the attributes in the processor.
   *
   * @param processors The processors
   * @param initialAttributes The attributes
   * @see RequireAttributes
   * @see ProvideAttributes
   */
  public static void fillProcessorAttributes(
      final List<Processor> processors, final Map<String, Attribute> initialAttributes) {
    Map<String, Attribute> currentAttributes = new HashMap<>(initialAttributes);
    for (Processor processor : processors) {
      if (processor instanceof RequireAttributes) {
        for (InputValue inputValue : ProcessorDependencyGraphFactory.getInputs(processor)) {
          if (inputValue.type == Values.class) {
            if (processor instanceof CustomDependencies) {
              for (String attributeName : ((CustomDependencies) processor).getDependencies()) {
                Attribute attribute = currentAttributes.get(attributeName);
                if (attribute != null) {
                  ((RequireAttributes) processor)
                      .setAttribute(attributeName, currentAttributes.get(attributeName));
                }
              }

            } else {
              for (Map.Entry<String, Attribute> attribute : currentAttributes.entrySet()) {
                ((RequireAttributes) processor)
                    .setAttribute(attribute.getKey(), attribute.getValue());
              }
            }
          } else {
            try {
              ((RequireAttributes) processor)
                  .setAttribute(inputValue.internalName, currentAttributes.get(inputValue.name));
            } catch (ClassCastException e) {
              throw new IllegalArgumentException(
                  String.format(
                      "The processor '%s' requires "
                          + "the attribute '%s' "
                          + "(%s) but he has the "
                          + "wrong type:\n%s",
                      processor, inputValue.name, inputValue.internalName, e.getMessage()),
                  e);
            }
          }
        }
      }
      if (processor instanceof ProvideAttributes) {
        Map<String, Attribute> newAttributes = ((ProvideAttributes) processor).getAttributes();
        for (OutputValue ouputValue : ProcessorDependencyGraphFactory.getOutputValues(processor)) {
          currentAttributes.put(ouputValue.name, newAttributes.get(ouputValue.internalName));
        }
      }
    }
  }

  private static void verifyAllMappingsMatchParameter(
      final Set<String> mappings, final Class<?> paramType, final String errorMessagePrefix) {
    final Set<String> attributeNames = ParserUtils.getAllAttributeNames(paramType);
    StringBuilder errors = new StringBuilder();
    for (String mapping : mappings) {
      if (!attributeNames.contains(mapping)) {
        errors.append("\n  * ").append(mapping);
      }
    }

    Assert.isTrue(
        errors.isEmpty(), errorMessagePrefix + errors + listOptions(attributeNames) + "\n");
  }

  private static String listOptions(final Set<String> attributeNames) {
    StringBuilder msg = new StringBuilder("\n\nThe possible parameter names are:");
    for (String attributeName : attributeNames) {
      msg.append("\n  * ").append(attributeName);
    }
    return msg.toString();
  }

  /**
   * Create a {@link ProcessorDependencyGraph}.
   *
   * @param processors the processors that will be part of the graph
   * @param attributes the list of attributes name
   * @return a {@link org.mapfish.print.processor.ProcessorDependencyGraph} constructed from the
   *     passed in processors
   */
  public ProcessorDependencyGraph build(
      final List<? extends Processor> processors, final Map<String, Class<?>> attributes) {
    ProcessorDependencyGraph graph = new ProcessorDependencyGraph();

    final Map<String, ProcessorGraphNode<Object, Object>> provideByProcessor = new HashMap<>();
    final Map<String, Class<?>> outputTypes = initialiseStringClassMapFrom(attributes);

    for (Processor processor : processors) {
      final ProcessorGraphNode<Object, Object> node =
          new ProcessorGraphNode<Object, Object>(processor, this.metricRegistry);

      final Set<InputValue> inputs = getInputs(node.getProcessor());
      // check input/output value dependencies
      boolean isRoot = isRoot(processor, inputs, outputTypes, provideByProcessor, node);
      if (isRoot) {
        graph.addRoot(node);
      }

      for (OutputValue value : getOutputValues(node.getProcessor())) {
        String outputName = value.name;
        if (outputTypes.containsKey(outputName)) {
          // there is already an output with the same name
          if (value.canBeRenamed) {
            // if this is just a debug output, we can simply rename it
            outputName = outputName + "_" + UUID.randomUUID().toString();
          } else {
            ProcessorGraphNode<Object, Object> provider = provideByProcessor.get(outputName);
            if (provider == null) {
              throw new IllegalArgumentException(
                  String.format(
                      "Processors '%s' provide the output '%s' who is already declared as "
                          + "an attribute.  You have to rename one of the outputs and the "
                          + "corresponding input so that there is no ambiguity with "
                          + "regards to the input a processor consumes.",
                      processor, outputName));
            } else {
              throw new IllegalArgumentException(
                  String.format(
                      "Multiple processors provide the same output mapping: '%s' and "
                          + "'%s' both provide: '%s'.  You have to rename one of the "
                          + "outputs and the corresponding input so that there is no "
                          + "ambiguity with regards to the input a processor consumes.",
                      processor, provider, outputName));
            }
          }
        }

        provideByProcessor.put(outputName, node);
        outputTypes.put(outputName, value.type);
      }

      // check input/output value dependencies
      for (InputValue input : inputs) {
        if (input.field.getAnnotation(InputOutputValue.class) != null) {
          provideByProcessor.put(input.name, node);
        }
      }
    }

    final Collection<? extends Processor> missingProcessors =
        CollectionUtils.subtract(processors, graph.getAllProcessors());
    final StringBuilder missingProcessorsName = new StringBuilder();
    for (Processor p : missingProcessors) {
      missingProcessorsName.append("\n- ");
      missingProcessorsName.append(p.toString());
    }
    Assert.isTrue(
        missingProcessors.isEmpty(),
        "The processor graph:\n"
            + graph
            + "\n"
            + "does not contain all the processors, missing:"
            + missingProcessorsName);

    return graph;
  }

  private boolean isRoot(
      final Processor processor,
      final Set<InputValue> inputs,
      final Map<String, Class<?>> outputTypes,
      final Map<String, ProcessorGraphNode<Object, Object>> provideByProcessor,
      final ProcessorGraphNode<Object, Object> node) {
    boolean isRoot = true;
    for (InputValue input : inputs) {
      if (input.name.equals(Values.VALUES_KEY)) {
        if (processor instanceof CustomDependencies) {
          for (String name : ((CustomDependencies) processor).getDependencies()) {
            final Class<?> outputType = outputTypes.get(name);
            if (outputType == null) {
              throw new IllegalArgumentException(
                  String.format(
                      "The Processor '%s' has no value for the dynamic input '%s'.",
                      processor, name));
            }
            final ProcessorGraphNode<Object, Object> processorSolution =
                provideByProcessor.get(name);
            if (processorSolution != null) {
              processorSolution.addDependency(node);
              isRoot = false;
            }
          }
        } else {
          for (ProcessorGraphNode<Object, Object> processorSolution : provideByProcessor.values()) {
            processorSolution.addDependency(node);
            isRoot = false;
          }
        }
      } else {
        final Class<?> outputType = outputTypes.get(input.name);
        if (outputType != null) {
          final Class<?> inputType = input.type;
          final ProcessorGraphNode<Object, Object> processorSolution =
              provideByProcessor.get(input.name);
          if (inputType.isAssignableFrom(outputType)) {
            if (processorSolution != null) {
              processorSolution.addDependency(node);
              isRoot = false;
            }
          } else {
            if (processorSolution != null) {
              throw new IllegalArgumentException(
                  String.format(
                      "Type conflict: Processor '%s' provides an output with name '%s' "
                          + "and of type '%s', while processor '%s' expects an input "
                          + "of that name with type '%s'! Please rename one of the "
                          + "attributes in the mappings of the processors.",
                      processorSolution.getName(),
                      input.name,
                      outputType,
                      node.getName(),
                      inputType));
            } else {
              throw new IllegalArgumentException(
                  String.format(
                      "Type conflict: the attribute '%s' of type '%s', while processor "
                          + "'%s' expects an input of that name with type '%s'!",
                      input.name, outputType, node.getName(), inputType));
            }
          }
        } else {
          if (input.field.getAnnotation(HasDefaultValue.class) == null) {
            throw new IllegalArgumentException(
                String.format(
                    "The Processor '%s' has no value for the input '%s'.", processor, input.name));
          }
        }
      }
    }
    return isRoot;
  }

  private Map<String, Class<?>> initialiseStringClassMapFrom(
      final Map<String, Class<?>> attributes) {
    final Map<String, Class<?>> outputTypes = new HashMap<>(attributes);

    // Add internal values
    outputTypes.put(Values.VALUES_KEY, Values.class);
    outputTypes.put(Values.TASK_DIRECTORY_KEY, File.class);
    outputTypes.put(
        Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, MfClientHttpRequestFactoryProvider.class);
    outputTypes.put(Values.TEMPLATE_KEY, Template.class);
    outputTypes.put(Values.PDF_CONFIG_KEY, PDFConfig.class);
    outputTypes.put(Values.SUBREPORT_DIR_KEY, String.class);
    outputTypes.put(Values.OUTPUT_FORMAT_KEY, String.class);
    outputTypes.put(Values.MDC_CONTEXT_KEY, Map.class);
    outputTypes.put(
        MapPrinterServlet.JSON_REQUEST_HEADERS, HttpRequestHeadersAttribute.Value.class);
    outputTypes.put(Values.LOCALE_KEY, Locale.class);
    return outputTypes;
  }
}
