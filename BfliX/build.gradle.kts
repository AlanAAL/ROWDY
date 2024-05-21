dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
}
// use an integer for version numbers
version = 4


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "Movies & TV Shows From BfliX"
    authors = listOf("Othmane")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1

    tvTypes = listOf("movies", "TvSeries")

    requiresResources = true
    language = "en"

    // random cc logo i found
    iconUrl = "https://bflix.to/assets/sites/bflix/favicon5.png"
}

android {
    buildFeatures {
        viewBinding = true
    }
}
