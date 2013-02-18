/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.opts;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper to parse a process' arguments and assign their values to a bean.
 * <p/>
 * Uses the {@link org.pvalsecc.opts.Option} annotation on the bean to know what
 * attributes are configurable and how.
 */
public class GetOptions {
    private static Pattern BOOLEAN = Pattern.compile("^--([^=]+)$");
    private static Pattern OTHER = Pattern.compile("^--([^=]+)=(.+)$");

    /**
     * Parses the arguments and assign their values to the provided bean
     *
     * @param args The arguments as passed to the "main" static method.
     * @param dest The bean to initialize.
     * @return List of non "--" arguments (that are ignored)
     * @throws InvalidOption In case of error.
     */
    public static List<String> parse(String[] args, Object dest) throws InvalidOption {
        List<String> remaining = new ArrayList<String>(5);
        Set<String> seen = new HashSet<String>();
        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];
                Matcher bool = BOOLEAN.matcher(arg);
                Matcher other = OTHER.matcher(arg);
                if (bool.matches()) {
                    String name = bool.group(1);
                    handleBoolean(dest, name);
                    seen.add(name);

                } else if (other.matches()) {
                    String name = other.group(1);
                    String value = other.group(2);
                    handleOther(dest, name, value);
                    seen.add(name);

                } else {
                    remaining.add(arg);
                }
            }
        }

        checkMandatory(dest, dest.getClass(), seen);

        return remaining;
    }

    public static String getShortList(Object dest) {
        StringBuilder result = new StringBuilder();
        final Class<?> aClass = dest.getClass();
        getShortList(result, aClass);
        return result.toString();
    }

    public static String getLongList(Object dest) throws IllegalAccessException {
        StringBuilder result = new StringBuilder();
        final Class<?> aClass = dest.getClass();
        getLongList(dest, result, aClass);
        return result.toString();
    }

    private static void checkMandatory(Object dest, Class<?> aClass, Set<String> seen) throws InvalidOption {
        if (aClass == Object.class) return;
        checkMandatory(dest, aClass.getSuperclass(), seen);
        for (Field field : aClass.getDeclaredFields()) {
            final Option annotation = field.getAnnotation(Option.class);
            final String name = field.getName();
            if (annotation != null && !seen.contains(name)) {
                if (annotation.environment().length() == 0) {
                    if (annotation.mandatory()) {
                        throw new InvalidOption("Mandatory option missing: " + name);
                    }
                } else {
                    String env = System.getenv(annotation.environment());
                    if (env != null) {
                        handleOther(dest, field.getName(), env);
                    } else if (annotation.mandatory()) {
                        throw new InvalidOption("Mandatory option missing: " + name);
                    }
                }
            }
        }
    }

    private static void getShortList(StringBuilder result, Class<?> aClass) {
        if (aClass == Object.class) return;
        getShortList(result, aClass.getSuperclass());
        for (Field field : aClass.getDeclaredFields()) {
            final Option annotation = field.getAnnotation(Option.class);
            if (annotation != null) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                if (!annotation.mandatory()) {
                    result.append("[");
                }
                if (field.getType() == boolean.class) {
                    result.append("--").append(field.getName());
                } else {
                    result.append("--").append(field.getName()).append("={value}");
                }
                if (!annotation.mandatory()) {
                    result.append("]");
                }
            }
        }
    }

    private static void getLongList(Object dest, StringBuilder result, Class<?> aClass) throws IllegalAccessException {
        if (aClass == Object.class) return;
        getLongList(dest, result, aClass.getSuperclass());
        for (Field field : aClass.getDeclaredFields()) {
            final Option annotation = field.getAnnotation(Option.class);
            if (annotation != null) {
                if (result.length() > 0) {
                    result.append('\n');
                }
                if (field.getType() == boolean.class) {
                    result.append("  --").append(field.getName());
                } else {
                    result.append("  --").append(field.getName()).append("={").append(field.getType().getSimpleName()).append("}");
                }
                result.append(": ").append(annotation.desc());
                if (!annotation.mandatory()) {
                    field.setAccessible(true);
                    result.append(" (defaults to [").append(field.get(dest)).append("])");
                }
            }
        }
    }

    private static void handleBoolean(Object dest, String name) throws InvalidOption {
        try {
            Field opt = getField(dest, name);
            if (opt.getAnnotation(Option.class) == null) {
                throw new InvalidOption("'" + name + "' is not an option");
            }
            opt.setAccessible(true);
            opt.setBoolean(dest, true);
        } catch (Exception e) {
            throw new InvalidOption("Unknown option '" + name + "'", e);
        }
    }

    private static void handleOther(Object dest, String name, String value) throws InvalidOption {
        try {
            Field opt = getField(dest, name);
            if (opt.getAnnotation(Option.class) == null) {
                throw new InvalidOption("'" + name + "' is not an option");
            }
            opt.setAccessible(true);
            if (opt.getType() == int.class) {
                opt.setInt(dest, Integer.parseInt(value));
            } else if (opt.getType() == double.class) {
                opt.setDouble(dest, Double.parseDouble(value));
            } else if (opt.getType() == float.class) {
                opt.setFloat(dest, Float.parseFloat(value));
            } else if (opt.getType() == boolean.class) {
                opt.setBoolean(dest, Boolean.parseBoolean(value));
            } else if (opt.getType() == String.class) {
                opt.set(dest, value);
            } else if (opt.getType() == Integer.class) {
                if (value.equalsIgnoreCase("null")) {
                    opt.set(dest, null);
                } else {
                    opt.set(dest, Integer.parseInt(value));
                }
            } else if (Enum.class.isAssignableFrom(opt.getType())) {
                opt.set(dest, Enum.valueOf((Class<Enum>) opt.getType(), value.toUpperCase()));
            } else if (Collection.class.isAssignableFrom(opt.getType())) {
                //the generics type is dropped during compilation. So we can just hope it's String.
                ((Collection) opt.get(dest)).add(value);
            } else {
                throw new InvalidOption("Unknown type for option " + name + ": " + opt.getType());
            }
        } catch (NoSuchFieldException e) {
            throw new InvalidOption("Unknown option " + name, e);
        } catch (IllegalAccessException e) {
            throw new InvalidOption("Unknown option " + name, e);
        } catch (IllegalArgumentException e) {
            throw new InvalidOption("Unknown value [" + value + "] for option " + name, e);
        } catch (RuntimeException e) {
            throw new InvalidOption("Unknown option " + name, e);
        }
    }

    private static Field getField(Object dest, String name) throws NoSuchFieldException {
        final Field field = getField(name, dest.getClass());
        if (field == null) throw new NoSuchFieldException("name");
        return field;
    }

    private static Field getField(String name, Class<?> aClass) {
        Field result = null;
        try {
            result = aClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            //ignored
        }
        if (result == null && aClass != Object.class) {
            result = getField(name, aClass.getSuperclass());
        }
        return result;
    }
}
