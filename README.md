# Parrot
A parrot would repeat what you said

The Parrot is a parrot, it can repeat what you said, actually, it can detect your voice and then record it, and play it, that is all. I use this to improve my English speaking, you know, I can hear what I said and then I can correct myself.

There is a video [here](https://www.youtube.com/shorts/k-6uZAbe6Ig), you can click the image down here to watch.

[![Watch the video](https://github.com/warriorWorld/Parrot/blob/master/app/screenshots/ss.jpg)](https://www.youtube.com/shorts/k-6uZAbe6Ig)

##### Technical detail(if you are not interested, just ignore this part)
First, denoise the audio input while recording. Put those data into a VAD SDK, find those active areas, then save it, and play the sound for users.
