Known breaking changes
----------------------

 - Layer order in JSON requests is reversed.
  - V2 - first layer is bottom layer of the map.
  - V3 - first layer is top layer of the map.
 - Scales, DPI moved from configuration to template/attributes in config.yaml file
 - http allowed hosts, headers, etc... moved to Http processors
