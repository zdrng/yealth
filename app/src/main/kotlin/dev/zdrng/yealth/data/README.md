# Data boundary

`healthconnect/` will contain the only direct dependency on the Health Connect client and
map SDK record types to domain models. `local/` is reserved for small on-device settings or
indexes if they become necessary. Health records are not duplicated by default.

