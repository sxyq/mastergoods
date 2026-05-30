pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "master-goods-android"

include(":app")
include(":backdrop")

include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:network")
include(":core:datastore")
include(":core:database")

include(":data:auth")
include(":data:product")
include(":data:customer")
include(":data:supplier")
include(":data:order")
include(":data:finance")
include(":data:report")
include(":data:agent")
include(":data:sync")

include(":feature:auth")
include(":feature:dashboard")
include(":feature:products")
include(":feature:customers")
include(":feature:suppliers")
include(":feature:sales")
include(":feature:purchases")
include(":feature:payments")
include(":feature:finance")
include(":feature:reports")
include(":feature:agent")
include(":feature:settings")
