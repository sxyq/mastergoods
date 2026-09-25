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
include(":benchmark")

include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:network")
include(":core:datastore")
include(":core:database")

include(":data:auth")
include(":data:agent")
include(":data:sync")

include(":feature:auth")
include(":feature:agent")
include(":feature:settings")
