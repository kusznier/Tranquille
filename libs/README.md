# Vendored libraries

## LibPhoneNumberInfo (`libs/LibPhoneNumberInfo`)

- Upstream: https://gitlab.com/xynngh/LibPhoneNumberInfo
- Version: commit `c77bdd2b692ac2c0a6e31dfa9ac73f4dd1d1a0ce` (2021-06-04),
  the same version that was previously fetched from JitPack.
- License: GNU AGPL v3.0, see `LibPhoneNumberInfo/LICENSE`. Tranquille is
  licensed under the same license.
- Imported unmodified with `git subtree --squash`. Only the `sia` module is
  built, as the Gradle subproject `:sia` (configured in the root
  `settings.gradle.kts` and `build.gradle.kts`).

To check the copy against upstream:

```
git fetch https://gitlab.com/xynngh/LibPhoneNumberInfo.git c77bdd2b692ac2c0a6e31dfa9ac73f4dd1d1a0ce
git diff FETCH_HEAD HEAD:libs/LibPhoneNumberInfo
```

To update, use `git subtree pull --prefix=libs/LibPhoneNumberInfo <url> <ref> --squash`
and review the diff, in particular anything under `sia/.../network/`.
