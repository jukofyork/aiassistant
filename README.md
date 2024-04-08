Fork of Wojciech Gradkowski's [AssistAI](https://github.com/gradusnikov/eclipse-chatgpt-plugin) for use with [Ollama](https://ollama.com).

---

# Installation

## To install the Plug-in Development Environment:
1. Go to Help -> Eclipse Matketplace.
2. Type "Eclipse PDE (Plug-in Development Environment)" in find textbox to search.
3. Install "Eclipse PDE (Plug-in Development Environment) latest".

## To install EGit:
1. Go to Help -> Eclipse Matketplace.
2. Type "EGit - Git Integration for Eclipse" in find textbox to search.
3. Install "EGit - Git Integration for Eclipse X.X.X".

## To import the project from GitHub:
1. Go to: *File → Import → Git → Projects from Git*.
2. Select “Clone URI” and click Next.
3. Enter https://github.com/jukofyork/aiassistant in the URI field.
4. (This should automatically fill the Host field as "github.com" and the Repositry path field as "/jukofyork/aiassistant").
5. Leave the Protocol as "https" and the Port blank.
6. Enter your GitHub username and a 'Personal Access Token' (**NOT** your GitHub password, see: https://github.com/settings/tokens).
7. Click finish.

## To build and install the plug-in:
1. Go to: *File → Export → Plug-in Development → Deployable plug-in and fragments*.
2. Select "eclipse.plugin.aiassistant (X.X.X qualifier)".
3. Set the destination as "Install into host repositry:" and leave as default ".../org.eclipse.pde.core/install/" location.
4. Click finish and restart the IDE when asked.

# Usage

Go to *Window → Show View → Other... → AI Assistant → AI Assistant* and click Open.

You should now see a view that looks like this:

![View](website/View.png?raw=true)

*NOTE: If the top part of the view is blank/grey then see below for how to fix this.*

Now click the "Settings" button.

If the [Ollama](https://ollama.com) API is set up correctly then should see something like this:

![Settings](website/Settings.png?raw=true)

Now right click some source code and you should see a context menu like this:

![Context Menu](website/ContextMenu.png?raw=true)

*NOTE: The "selection" the context menu uses will be the whole text of the current editor window if nothing is selected.*

*NOTE: "Fix Warnings" and "Fix Errors" will only appear if you have a file selected with compiler warnings or errors in it.*

and you should see something like this when the AI Assistant runs:

![Example](website/Example.png?raw=true)

You can also paste in code or documentation using the clipboard by right clicking the main view like this:

![PasteMenu](website/PasteMenu.png?raw=true)

## View Navigation
- Holding the Shift key whilst srolling will scroll through messages.
- Holding the Control key whilst srolling will scroll to the top or bottom.

## Input Area
- Pressing Enter will send the current user messaage to the AI Assistant.
- Pressing Shift+Enter will insert a newline into a message instead of sending it.
- Pressing Control+Enter will add a new user message, but delay sending to the AI Assistant.
- The yellow up/down arrows can be used to retrieve previously sent user messages.

## Code Block Buttons
![BrowserButons](website/BrowserButons.png?raw=true)
- **Copy Code**: Copies the code block to the clipbpard.
- **Replace Selection**: Copies the code block over the top of the curremt editor selection.
- **Review Changes**: Opens a compare editor that lets you selectively replace the curremt editor selection.
- **Apply Patch**: Opens the apply patch wizard (NOTE: Only shows if the code block is marked as `diff` language).

*NOTE: The "selection" the buttons use will be the whole text of the current editor window if nothing is selected.*

*NOTE: Make sure to set "Ignore white space" in Compare/Patch settings in Eclipse or (any) compare editor won't work well.*

## Custom Prompt Templates
- The templates use the [StringTemplate](https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md) library.
- The following context variables can be used: `<taskname>`, `<usertext>`, `<filename>`, `<language>`, `<tag>`, `<warnings>`, `<errors>`, `<document>`, `<clipboard>`, `<selection>`, `<lines>` and `<documentation>` (see the sourcecode of the class `Context` for what each one does).
- The special `<<switch-roles>>` tag can be used for delaying responses, forcing responses, multi-shot learning, etc.

---

## How to fix the blank/grey SWT Browser widget bug

Set the environment variables `WEBKIT_DISABLE_COMPOSITING_MODE=1` and/or `WEBKIT_DISABLE_DMABUF_RENDERER=1` before running Eclipse, eg:

**Linux**
```
#!/bin/bash
export WEBKIT_DISABLE_COMPOSITING_MODE=1
export WEBKIT_DISABLE_DMABUF_RENDERER=1
./eclipse
```

**Windows**
```
@ECHO OFF
SET WEBKIT_DISABLE_COMPOSITING_MODE=1
SET WEBKIT_DISABLE_DMABUF_RENDERER=1
start eclipse.exe
```
