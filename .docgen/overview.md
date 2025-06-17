You can use the `{{ .Plugin.ID }}` policy to override headers in incoming or outgoing traffic.

### Execution order
Header transformations are executed in the following order:

1. Set/replace headers
2. Append headers: add values to existing headers or add a new header
   * This is not supported for Native APIs
3. Remove headers
4. Keep only whitelisted headers

### Header removal
* Headers added/appended by this policy can be removed
* Whitelisting applies to headers added/appended by this policy
