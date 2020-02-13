This example demonstrates a more complex configuration containing:

> -   A map containing a wms layer and a vector layer
> -   A DataSourceProcessor containing a TableProcessor which is configured to have a dynamic number of
>     columns
>     \* The table is also configured so that any column with the name "icon" will be converted to an image.
> -   A Legend
> -   A North Arrow
> -   A Scalebar

The most significant part of this example is the DataSourceProcessor. This configuration allows for an
arbitrary number of tables and each table can have an arbitrary number of columns and rows.

The purpose of a DataSourceProcessor is to allow an arbitrary number of rows, be they tables, maps, whatever

A Table can be configured as dynamic, which means that the number of columns is not know ahead of time and the
subtemplate will be generated with the correct number of columns for the actual data.
