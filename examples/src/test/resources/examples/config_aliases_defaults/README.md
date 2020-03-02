This example demonstrates aliasing and attribute defaults.

YAML Aliasing
=============

Yaml has a feature called "Aliases" which allows the same object to be used in multiple places by
\_[tagging]() the object of interest with an alias and then \_[dereferencing]() the alias later when one
wishes to reuse the objct.

The purpose of YAML aliasing in a YAML file is to reduce duplication in the files.

A YAML object can be tagged with a &TAG\_NAME and then reused using \*TAG\_NAME as a dereference. It is
important to remember that this is *not* a copy of the object, it is the same object, thus any changes to the
object (by a setter for example) will affect all referencing instance.

For example:

:   maxDpi: &MAX\_DPI 254 \# declaring the alias MAX\_DPI ref: \*MAX\_DPI \# dereferencing the alias

Defaults
========

This example also demonstrates the use of default attribute values. All Attributes have an optional property
called default. This mechanism allows default values to be set in the configuration file and allows the client
to ignore those attributes unless it wants to specifically override the default values.
