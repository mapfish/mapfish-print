package org.mapfish.print.attribute.map;

import org.mapfish.print.config.Template;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
public final class TestMapAttribute extends GenericMapAttribute<TestMapAttribute.TestMapAttributeValues> {

    @Override
    protected Class<TestMapAttributeValues> getValueType() {
        return TestMapAttributeValues.class;
    }

    @Override
    public TestMapAttributeValues createValue(final Template template) {
        return new TestMapAttributeValues(template);
    }

    public final class TestMapAttributeValues extends GenericMapAttribute<?>.GenericMapAttributeValues {
        public int requiredElem;
        public int[] requiredArray;
        public EmbeddedTestAttribute embedded;
        public PArray pArray;
        public PObject pObject;

        @HasDefaultValue
        public EmbeddedTestAttribute optionalEmbedded;
        @HasDefaultValue
        public int[] optionalArray;

        public TestMapAttributeValues(Template template) {
            super(template, null);
            optionalArray = new int[]{1, 2};
        }

        @Override
        public Double getDpi() {
            return null;
        }

        @Override
        protected PArray getRawLayers() {
            return null;
        }
    }

    public static final class EmbeddedTestAttribute {
        public boolean embeddedElem;
    }

}
