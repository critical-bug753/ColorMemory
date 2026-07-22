# Chroma Master

**Match the hex. Master the color.**

Chroma Master is an Android color-guessing game where you test your eye for color by matching hex codes across three difficulty modes. Can you get a perfect 1000?

## How to Play

1. **Choose a mode**
   - **Easy** — match a single random color
   - **Moderate** — match a color within the same hue family
   - **Difficult** — memorize a sequence of 5 shades, then recall the target

2. **Memorize** the target color shown during the preview phase.

3. **Pick your guess** using the HSV color picker and fine-tune with the hue slider.

4. **Submit** before the 5-second timer runs out.

5. **Score** is based on color accuracy and speed. Play 10 rounds and see your final tally.

## Features

- 3 difficulty modes with distinct color-generation logic
- Live HSV color picker with real-time hex preview
- Countdown timer with progress indicator
- Per-round scoring and animated results
- Final summary with round breakdown, best guess, and performance rating
- High-score persistence with DataStore
- Share your final score as an image

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3
- ViewModel + StateFlow
- Room-like persistence via DataStore Preferences

## Getting Started

```bash
git clone https://github.com/<your-username>/ColorMemory.git
cd ColorMemory
./gradlew assembleDebug
```

Then install the generated APK on an Android device or emulator.
Or else I have provided an apk file which can be downloaded and installed on an Android device or emulator

## Contributing

Pull requests are welcome. Feel free to open issues for bugs or feature requests.

## License

Critical_Bug
