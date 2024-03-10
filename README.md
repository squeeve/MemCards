

MEMCARDS:
=========

# Usage:

This is an Android app that first asks a user if they want to play an Easy, Medium, or Hard game -- 2x2, 4x4, or 6x6 grids of memory cards, respectively -- and then keeps track of how many tries it takes a user to complete the board. That's it. This is my first Android app ever, so there are code hacks and bad practices that are simply the result of still feeling my way around app development.


## Things I learned from this project:

- How the Layouts and Activities (and res objects) work together.
- Navigating between Activities via Intent()
- Using the `AndroidManifest.xml` to expose or hide actvities/entry points.
- Views that are composed of other views. For example, in this project, GridLayout contains a set of TextViews, which are presented according to the `columnCount` and `rowCount`.
- Utilizing themes and styles to make views cleaner.
- When to implement setOnClickListener inline, versus as a separate member function, versus outside of any classes, as a companion function. (Is that Kotlin-specific?)
- Using Toast (pop-ups) to debug my code. Haha..


## Minor other bits and pieces that I experienced in passing:

- Kotlin doesn't like getters and setters the way Java does. It also just does things with a lot less code.
- camelCase for variable/resource names, snake_case for filenames, PascalCase for class names.
- JetBrains IDE makes trying out code from other people's repos pretty easy; it's frequently a lot nicer to browse code from the IDE than from Github.
- The "Compat" classes abstract away the version-specific bits of coding that would otherwise be required to run on different platforms.


## Other things to explore:

- How do you use a companion function?
- RecyclerView is a cool way to create lots of scroll-view-friendly items, and is called "recycler" because it trashes Views that go away as it creates new Views to display. This is something I'll want to explore more, as a case study.
- The importance of separating Data from View, and using an Adapter and Binder to put them together.
- How do you unit-test?
- Animation?
- How to pause a display before changing it... case in point: I had a hard time showing the second open card because if it didn't match with the first card, it flipped back around and the user never sees it.
