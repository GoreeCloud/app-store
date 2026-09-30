# GoreeCloud App Store Branding Contract

GoreeCloud App Store consumes branding from the canonical `GoreeCloud/goreecloud-branding-assets` repository.

The branding repository remains authoritative. Consumer copies in this repository are implementation derivatives only and do not create new branding authority.

## App Store identity

The approved canonical App Store product identity is:

- canonical asset: `products/app-store/app-icon.svg`
- canonical Git blob: `1e86041de7cbde9f92ae2ddb9a813b2585b5f788`
- Android derivative: `app/src/main/res/drawable/goreecloud_app_store_icon.xml`
- Android manifest consumer: `android:icon="@drawable/goreecloud_app_store_icon"`

The current mobile identity uses the GoreeCloud blue/deep-blue family with a frosted software-acquisition bag/download glyph. It remains distinct from GoreeCloud Launcher and GoreeCloud Search and must not be replaced by Apple App Store, Google Play, F-Droid, shopping-cart, physical-storefront, generic package-manager, framework, or emoji artwork.

Branding does not establish entitlement, package authorization, artifact verification, successful download/install, security, privacy, continuity, or release truth.

## Approved catalog derivatives

The native development catalog renders first-party artwork from traceable Android VectorDrawable derivatives of approved canonical sources:

| Store item | Canonical asset | Canonical Git blob | Android derivative |
| --- | --- | --- | --- |
| GoreeCloud Browser | `products/browser/app-icon.svg` | `2a81cc68cb8c1831dfd7bec6c3d0b14e2f421f1f` | `app/src/main/res/drawable/goreecloud_browser_icon.xml` |
| GoreeCloud Messenger | `products/messenger/app-icon.svg` | `01102af91a43e100c66877489b94929165ec0430` | `app/src/main/res/drawable/goreecloud_messenger_icon.xml` |
| GoreeCloud Location | `products/location/app-icon.svg` | `ceb93b6d814c80ece0929022eb5edcdfbc346e2d` | `app/src/main/res/drawable/goreecloud_location_icon.xml` |
| GoreeCloud Manager | `products/manager/app-icon.svg` | `024d82d5b5911e426216dfbd6a19d95cd6d71fc3` | `app/src/main/res/drawable/goreecloud_manager_icon.xml` |
| Identity Center | `services/identity-center/service-icon.svg` | `36922e5a747817267a27f640bb4234b8d59ab2a5` | `app/src/main/res/drawable/goreecloud_identity_center_icon.xml` |
| Mesh Center | `services/mesh-center/service-icon.svg` | `2628ff825549847398e98d9768f8f57b30aa378a` | `app/src/main/res/drawable/goreecloud_mesh_center_icon.xml` |

Identity Center must not reuse the complete `products/identity/app-icon.svg` application identity. Mesh Center must not fall back to a generic cloud/network glyph. The Mesh Center derivative follows the approved GoreeCloud Mesh **Interlace** Identity DNA while remaining a reduced service-specific mark. Both service identities remain reduced derivatives of their registered parent Identity DNA, and runtime state is communicated separately from the stable mark.

## Consumer-derivative rules

- Local Android resources are packaging derivatives only; canonical SVGs and their pinned Git blobs remain authoritative.
- A derivative must preserve the defining canonical geometry and color relationship within Android VectorDrawable constraints.
- Service artwork must not be substituted with its parent application/system icon merely because that parent asset exists locally.
- Generic framework glyphs may be used for navigation or unlabeled UI affordances where semantically appropriate, but not as substitutes for a registered first-party catalog item identity.
- Operational state such as connected, authorized, healthy, secure, synchronized, installed, or verified must not be encoded into stable product/service identity artwork.
- Any canonical asset revision requires an explicit provenance update and regenerated derivative; local edits never become independent branding authority.

## Validation requirements

Repository validation must fail closed if:

- the App Store launcher derivative or `android:icon` mapping disappears;
- the canonical App Store blob reference drifts;
- Identity Center maps back to the GoreeCloud Identity application icon;
- Mesh Center loses its approved service derivative or regresses to the generic service fallback;
- required service/product derivative files disappear;
- the defining canonical color/geometry tokens used by the reviewed derivatives drift without a corresponding approved canonical update.

This branding integration resolves artwork/source-package identity gaps only. It does not establish production signing, package-delivery acceptance, GoreeCloud Identity authorization, runtime service availability, complete Glaze UI conformance, production deployment, or Stable release qualification.
