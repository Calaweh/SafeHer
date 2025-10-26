package com.example.safeher.data.injection

//@Module
//@InstallIn(SingletonComponent::class)
//object CoilModule {
//    @Provides
//    @Singleton
//    fun provideImageLoader(
//        @ApplicationContext context: Context,
//        mediaRemoteDataSource: MediaRemoteDataSource
//    ): ImageLoader {
//        Log.d("CoilModule", "Creating ImageLoader")
//        val imageLoader = ImageLoader.Builder(context)
//            .components {
//                add(FirestoreImageFetcher.Factory(mediaRemoteDataSource)) // Line ~37
//                add(object : Fetcher.Factory<Any> {
//                    override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
//                        Log.d("CoilDebug", "Fetcher tried with data: $data, type: ${data::class.java}")
//                        return null
//                    }
//                })
//                Log.d("CoilModule", "FirestoreImageFetcher.Factory added")
//            }
//            .logger(DebugLogger(Log.VERBOSE))
//            .build()
//        Log.d("CoilModule", "ImageLoader created: $imageLoader")
//        return imageLoader
//    }
//}