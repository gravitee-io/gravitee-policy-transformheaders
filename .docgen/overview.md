You can use the `{{ .Plugin.ID }}` policy to override headers in incoming or outgoing traffic.

### Execution order
Configured transformations headers are executed in the following order:

1. Set / Replace headers
2. Append values to existing headers (will add it if absent)
3. Remove headers
4. Keep only whitelisted headers

### Removal
* Headers to removed include added/appended one by this policy
* Whitelisting applies also to added/appended headers by this policy

### Native APIs

Appending a list of values for a given header is not supported for Native APIs.

