# LazySlideshow
A lazy way to turn old android devices into digital photo frames or to switch periodically between apps.

The idea behind this project is to simply turn my tablet that hangs on my wall over my monitor into a gallery.
The reason for this is to remind myself daily of the goals I am trying to achieve, a form of manifestation.
Later on I also added the option of switching between apps when in an unlocked state.

The app starts by itself automatically on device boot and when it's next configured to do so.
The configurations can be found in `Configurations.kt`, here you can change if it runs work days only and when the app starts & stops.

Once the app starts it will display each image that is present in `res/values/images.xml`, followed by each app/url in `res/values/apps.xml`

Feel free to fork/pull as you please.