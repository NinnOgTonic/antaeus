plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation("joda-time:joda-time:2.10.9")
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))

}