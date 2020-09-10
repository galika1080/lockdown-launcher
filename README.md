# lockdown-launcher
An Android launcher to keep you focused

I started working on this project after I realized how frequently I picked up my phone and wasted time while trying to be productive. A quick five-minute break would turn into ten minutes, a few more breaks and I'd have spent half an hour doing nothing... the cycle had to be broken. Lockdown Launcher puts a small but inconvenient barrier in front of opening distracting apps, like a delay (surprisingly effective) or a password (not particularly effective). The launcher is broken down into pages of apps, as are most, and these pages can be individually designated as lockable or not. When the launcher is locked, only these pages become restricted, while the rest of the phone's functionality is unhindered.

## downloading
If you want to fiddle with my code and build lockdown-launcher yourself, fork or clone this repo and open the project in Android Studio. If you just want to download a built release though, download it from google play [here](https://play.google.com/store/apps/details?id=agalik.lockdownlauncher).

## screenshots and design considerations
### pages and folders
Each page has a number of folders. App icons are hidden behind text so the launcher isn't so flashy. The number of folders per page can be configured in a global setting, but I'd like to move towards each page being individually configurable.

<img src="/screenshots/layout.png" alt="four closed folders" width="200" height="400">

Lo and behold, folders contain apps. I'm considering adding labels to the app icons to match the behavior of other launchers, but it may look better and still be usable without extra text.

<img src="/screenshots/folderopen.png" alt="one folder open" width="200" height="400">

### locked pages
Pages are lockable to prevent access to any apps they contain. Currently, all lockable pages are locked at once, which seems sufficient; if you're trying to be productive, why would having access to half your distracting apps be okay? I would, however, like to change the way lockability is handled. At the moment, a page either responds or ignores the global lock signal based on its index, meaning all permanantly unlocked pages are before all lockable ones.

<img src="/screenshots/locked.png" alt="locked folder" width="200" height="400">

### app drawer
As it stands, the app drawer is wholly unpretentious... to the point where it lacks some much needed polish. Tapping an app icon brings up a context menu, a little like long-pressing apps on the pixel 3 launcher. The menu has two options: "launch", which opens the app, and "place", which lets the user tap on any icon slot and put the app there. While it may seem obvious to have a tap open the app and a long-press begin placing it, this becomes unintuitive when the user taps on an empty icon slot (these have plus icons on them) and is brought to the app drawer. I suppose the widget menu on many launchers has gotten away with this minor flaw for years...

<img src="/screenshots/drawer.png" alt="locked app drawer" width="200" height="400">

### settings
Settings are great, but at a certain point, it gets messy and intimidating to have too many in the same place. I'm considering moving some of the global settings, like folders per page, to the pages themselves. This fits in with my goal of having each page be more individually configurable while making it more obvious how to change such layout options. The manner of selecting which pages are lockable also seems a little clumsy, and again, having a switch for it on each page would likely make it much better.

<img src="/screenshots/settings.png" alt="settings menu" width="200" height="400">
