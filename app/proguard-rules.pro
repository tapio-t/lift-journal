# No app-specific rules are needed: backups are read and written with kotlinx.serialization's JsonElement API,
# which uses no reflection, and the libraries ship their own R8 rules.
