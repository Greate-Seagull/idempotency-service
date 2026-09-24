# Changelog

## 1.0.0 (2026-09-24)


### ⚠ BREAKING CHANGES

* **idempotency-spring-boot-starter:** components.exception.ConnectionException has been removed. Consumers catching this class directly must update imports to use exception.ConnectionException from idempotency-api.

### Features

* add an auto configuration source ([8add2c4](https://github.com/Greate-Seagull/idempotency-service/commit/8add2c4353e6dd45ed997ef543c02c6c5ad0740c))
* add generic exception for connection fails ([e1ff638](https://github.com/Greate-Seagull/idempotency-service/commit/e1ff6380d21f0e821f5a172fa4bd4b1e439ca95f))
* build idempotency service to orchestrate main logic ([37075a9](https://github.com/Greate-Seagull/idempotency-service/commit/37075a92cc39d244533d5dd45608486f0ff7be38))
* **idempotency-spring-boot-starter:** wire retry support and fail-open/fail-closed handling into Spring Boot starter ([c259632](https://github.com/Greate-Seagull/idempotency-service/commit/c259632a29656b2311a815980ad928a3a7f9e549))
* implement hasher using native MessageDigest and Jackson ([6a830ec](https://github.com/Greate-Seagull/idempotency-service/commit/6a830ec03e1f0a8ed3d41314abc9ce9f62f80075))
* implement idempotency store using redis ([29709e9](https://github.com/Greate-Seagull/idempotency-service/commit/29709e9459364408efd038475d23380216e494b3))
* implement serializer using Jackson mapper ([4dd3347](https://github.com/Greate-Seagull/idempotency-service/commit/4dd33475202df227adc01ec4fedc2d9334b7843f))
* make a configuration properties ([64236ab](https://github.com/Greate-Seagull/idempotency-service/commit/64236ab2bd6a75f60cfb93609016f4a598f12773))
* provide an idempotency contract builder for rest endpoint ([842211d](https://github.com/Greate-Seagull/idempotency-service/commit/842211d5632e28c669bdae5f9bb868eaa6625f17))


### Bug Fixes

* **ci:** correct branch trigger from main to master ([af6ec60](https://github.com/Greate-Seagull/idempotency-service/commit/af6ec60fc91e2a1b45292493c84877187eccc5a4))
* **ci:** correct branch trigger from main to master ([6df0810](https://github.com/Greate-Seagull/idempotency-service/commit/6df08106df15d9090f1163236e483cecbd9aa1b9))
* **idempotency-spring-boot-starter:** remove Spring Boot plugin to fix bootJar build failure ([4299789](https://github.com/Greate-Seagull/idempotency-service/commit/42997893ef4fea16331ef852d14548eab51dc8e3))
