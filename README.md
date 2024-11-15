# WALKR - Mixed Reality Navigation Tool

![WALKR Logo](INSERT IMAGE HERE)

WALKR is an award-winning mixed reality navigation tool that merges augmented reality and geospatial mapping to provide real-time, hands-free navigation assistance. Originally developed for the HACKRPI 2024 hackathon, WALKR uses cutting-edge AR and voice-recognition technologies to enhance accessibility and ease of navigation, especially in dense urban environments.

## Demo

[![Check out a video demo of WALKR in action!](https://img.youtube.com/vi/LTzdAMWf2bk/maxresdefault.jpg)](https://youtu.be/LTzdAMWf2bk)

## Inspiration
Navigating unfamiliar cities can be overwhelming, especially for those with mobility challenges. WALKR was designed to empower users to explore urban spaces confidently without constant reference to a map. Inspired by the 12% of the U.S. population affected by mobility disabilities, WALKR’s intuitive, accessible interface creates a reliable and inclusive wayfinding experience for all.

## Features
- **Hands-Free Voice Control**: Initiate navigation by simply saying your destination.
- **Mixed Reality Path Guidance**: AR overlays with directional markers show the way without looking at the screen.
- **Wheelchair-Accessible Routing**: Customized route options prioritize accessibility.
- **Real-Time Positioning and Orientation**: Utilizes GPS, phone angle, and orientation for accurate path alignment with surroundings.

## Tech Stack
- **Languages**: Java, Kotlin
- **Frameworks & Libraries**: Android SDK, Google Maps, ARCore, Google Directions API, Google Places API
- **Voice Recognition**: Vosk
- **Additional Tools**: ML-powered geospatial processing

## How It Works
1. **Route Initiation**: Users can speak their destination aloud, allowing WALKR to set up a precise route hands-free.
2. **Location and Orientation**: WALKR instantly pinpoints the user’s location, integrating with geospatial mapping and GPS for real-time updates.
3. **Path Overlay in Mixed Reality**: Through Google Cardboard, WALKR displays an AR path aligned with the user’s direction, showing clear markers for easy wayfinding.

## Challenges Overcome
- **Wheelchair-Accessible Routing**: Custom routing logic was built to overcome limitations in the Google Maps API.
- **Technical Integration Under Time Constraints**: Developed within 24 hours, our team of three tackled multiple complex APIs and new programming languages to deliver a functional, reliable product.

## Installation & Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/Unbounder1/WALKR.git
   cd WALKR
   ```
2. Open the project in Android Studio.
3. Connect an Android device and launch the app from Android Studio.

> **Note:** You’ll need a Google Cardboard-style headset for the mixed reality experience.

## Usage
1. **Insert your phone into the headset** and launch WALKR.
2. **Speak your destination** to initiate the route.
3. Follow the **AR path markers** displayed on the screen to navigate effortlessly.

## Future Plans
- **iOS Compatibility**: Expanding to support iOS and other platforms.
- **Proprietary Headset Development**: Aiming to offer a universally compatible hands-free experience.
- **Enhanced Accessibility**: Continued refinement of accessible route options to ensure the best experience for all users.

## Contribution
We welcome contributions! Please fork the repository and submit a pull request with your changes.

## License
Distributed under the MIT License. See `LICENSE` for more information.