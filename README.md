# AI Assistant for Eclipse

An AI-powered coding assistant plugin for the Eclipse IDE based on a fork of Wojciech Gradkowski's [AssistAI](https://github.com/gradusnikov/eclipse-chatgpt-plugin).

## Table of Contents

- [Features](#features)
- [System Requirements](#system-requirements)
- [Installation](#installation)
  - [Prerequisites](#prerequisites)
  - [Installing from Source](#installing-from-source)
- [Quick Start](#quick-start)
- [Initial Setup](#initial-setup)
  - [Opening the AI Assistant View](#opening-the-ai-assistant-view)
  - [First-Time Configuration](#first-time-configuration)
  - [Supported AI Providers](#supported-ai-providers)
  - [Bookmarked API Settings](#bookmarked-api-settings)
- [Main Interface](#main-interface)
  - [Chat Conversation Area](#chat-conversation-area)
  - [User Input Area](#user-input-area)
  - [Button Bar](#button-bar)
- [Code Analysis Features](#code-analysis-features)
  - [Editor Context Menu](#editor-context-menu)
  - [Adding Context](#adding-context)
- [Interactive Code Blocks](#interactive-code-blocks)
  - [Review Changes Dialog](#review-changes-dialog)
  - [Apply Patch Wizard](#apply-patch-wizard)
- [Git Integration](#git-integration)
  - [Team Context Menu](#team-context-menu)
  - [Analysing File-Level Git Diffs](#analysing-file-level-git-diffs)
- [Chat Area Context Menu](#chat-area-context-menu)
- [Navigation & Shortcuts](#navigation-shortcuts)
  - [Keyboard Shortcuts](#keyboard-shortcuts)
  - [Mouse Navigation](#mouse-navigation)
- [Advanced Features](#advanced-features)
  - [Reasoning Model Support](#reasoning-model-support)
  - [LaTeX Mathematical Content](#latex-mathematical-content)
  - [Conversation Management](#conversation-management)
- [Customisation](#customisation)
  - [Prompt Template System](#prompt-template-system)
  - [JSON/TOML Overrides](#json-toml-overrides)
  - [UI Preferences](#ui-preferences)
- [Best Practices](#best-practices)
  - [Effective Prompting](#effective-prompting)
  - [Workflow Integration](#workflow-integration)
  - [Performance Tips](#performance-tips)
- [Troubleshooting](#troubleshooting)
  - [Blank/Grey Browser Widget](#blank-grey-browser-widget)
  - [Common Issues](#common-issues)
- [Security Considerations](#security-considerations)
- [FAQ](#faq)
- [Contributing](#contributing)
  - [Architecture](#architecture)
  - [Adding Features](#adding-features)
- [License](#license)

## Features

- **Multiple AI Provider Support**: Works with OpenAI, OpenRouter, Ollama, llama.cpp, and other OpenAI-compatible APIs
- **Advanced Code Analysis**: Comprehensive code review, explanation, debugging, and optimization capabilities
- **Interactive Code Blocks**: Copy, replace, and review code changes directly from AI responses
- **Git Integration**: Analyse diffs and generate commit messages from staged changes
- **Customisable Prompts**: Full template system with context-aware variables
- **Reasoning Model Support**: Special support for reasoning models with thinking blocks
- **Rich Text Rendering**: Code syntax highlighting and LaTeX math support
- **Conversation Management**: Import/export conversations, full undo/redo support

## System Requirements

- **Eclipse IDE**: 2022-03 or later
- **Java**: 17 or later
- **Operating System**: Windows, macOS, or Linux
- **Memory**: Minimum 4GB RAM recommended for large conversations
- **Network**: Internet connection for cloud AI providers (not required for local providers)

## Installation

### Prerequisites

1. **Eclipse PDE (Plug-in Development Environment)**:
   - Go to Help → Eclipse Marketplace
   - Type "Eclipse PDE (Plug-in Development Environment)" in find textbox to search
   - Install "Eclipse PDE (Plug-in Development Environment) latest"

2. **EGit - Git Integration for Eclipse**:
   - Go to Help → Eclipse Marketplace
   - Type "EGit - Git Integration for Eclipse" in find textbox to search
   - Install "EGit - Git Integration for Eclipse X.X.X"

### Installing from Source

1. **Import the project from GitHub**:
   - Go to: *File → Import → Git → Projects from Git*
   - Select "Clone URI" and click Next
   - Enter https://github.com/jukofyork/aiassistant in the URI field
   - (This should automatically fill the Host field as "github.com" and the Repository path field as "/jukofyork/aiassistant")
   - Leave the Protocol as "https" and the Port blank
   - Enter your GitHub username and a 'Personal Access Token' (**NOT** your GitHub password, see: https://github.com/settings/tokens)
   - Click finish

2. **Build and install the plugin**:
   - Go to: *File → Export → Plug-in Development → Deployable plug-in and fragments*
   - Select "eclipse.plugin.aiassistant (X.X.X qualifier)"
   - Set the destination as "Install into host repository:" and leave as default ".../org.eclipse.pde.core/install/" location
   - Click finish and restart the IDE when asked

## Quick Start

1. Install the plugin following the [Installation](#installation) instructions
2. Open the AI Assistant view: *Window → Show View → Other... → AI Assistant → AI Assistant*
3. Click the "Settings" button and configure your AI provider
4. Right-click in any code editor and try "AI Assistant → Explain"
5. Start coding with AI assistance!

## Initial Setup

### Opening the AI Assistant View

1. Go to Window → Show View → Other...
2. Expand "AI Assistant" category
3. Select "AI Assistant" and click Open

![Show View Dialog](website/show-view-dialog.png?raw=true)

### First-Time Configuration

1. Click the "Settings" button in the AI Assistant view
2. Configure your AI provider settings:
   - **Model Name**: The specific model to use (e.g., `llama3.1:8b`, `gpt-4`, `claude-3-sonnet`)
   - **API URL**: The base URL for your AI provider
   - **API Key**: Your authentication key (leave blank for local providers like Ollama)
   - **JSON Overrides**: Additional API parameters in JSON or TOML format

![Main Preferences Page](website/preferences-main-page.png?raw=true)

### Supported AI Providers

The plugin supports multiple AI providers with pre-configured examples:

| Provider | Example URL | Notes |
|----------|-------------|-------|
| OpenAI | `https://api.openai.com/v1` | Supports all OpenAI models including o1 reasoning models |
| OpenRouter | `https://openrouter.ai/api/v1` | Access to Anthropic Claude and other models |
| Ollama | `http://localhost:11434/v1` | Local model hosting |
| llama.cpp | `http://localhost:8080/v1` | Local model hosting |
| TabbyAPI | `http://localhost:5000/v1` | Local model hosting |

### Bookmarked API Settings

Use the bookmarked settings table to quickly switch between different AI providers and models:

- **Bookmark**: Save current settings for easy switching
- **Populate**: Automatically discover available models from your API provider
- **Sort**: Organise bookmarks alphabetically
- **Clear**: Remove all bookmarked settings

## Main Interface

![Chat Conversation Example](website/main-interface-example.png?raw=true)

### Chat Conversation Area

The main chat area displays conversations between you and the AI assistant. Messages are color-coded:
- **User messages**: Darker background
- **Assistant responses**: Lighter background  
- **Notifications**: System messages and status updates

### User Input Area

- **Enter**: Send message and request AI response
- **Shift+Enter**: Insert newline without sending
- **Ctrl+Enter**: Send message without requesting immediate response
- **Arrow buttons**: Navigate through previous messages

### Button Bar

- **Stop**: Cancel current AI request
- **Clear**: Clear conversation history
- **Undo**: Remove last interaction
- **Redo**: Restore undone interaction
- **Import**: Load conversation from JSON file
- **Export**: Save conversation as JSON or Markdown
- **Settings**: Open preferences dialog

## Code Analysis Features

### Editor Context Menu

Right-click in any code editor to access AI-powered analysis tools:

![Editor Context Menu](website/editor-context-menu.png?raw=true)

#### Code Understanding
- **Discuss**: Open-ended discussion about code
- **Explain**: Detailed explanation of code functionality

#### Code Quality & Review  
- **Code Review**: Comprehensive code analysis and suggestions
- **Best Practices**: Check adherence to coding standards
- **Robustify**: Improve error handling and edge cases
- **Debug**: Identify potential bugs and issues

#### Code Transformation
- **Optimize**: Performance improvement suggestions
- **Refactor**: Restructure code for better maintainability
- **Write Comments**: Generate documentation and comments
- **Code Generation**: Create boilerplate code
- **Code Completion**: Fill in missing implementations

#### Error Resolution
When Eclipse detects compiler errors or warnings, additional options appear:
- **Fix Errors**: Address compilation errors
- **Fix Warnings**: Resolve compiler warnings  
- **Fix Errors and Warnings**: Address both simultaneously

![Context Menu with Errors](website/context-menu-with-errors.png?raw=true)

### Adding Context

- **Add To Message**: Include code/file in your message
- **Add As Context**: Provide background information for AI analysis
- **Add Staged Diff**: Include Git changes in your request

![Adding Context Example](website/adding-context-example.png?raw=true)

## Interactive Code Blocks

AI responses containing code include interactive buttons:

![Code Block Buttons](website/code-block-buttons.png?raw=true)

- **Copy Code**: Copy to clipboard
- **Replace Selection**: Replace selected code in editor
- **Review Changes**: Open side-by-side comparison

![Code Block Patch Button](website/code-block-patch-button.png?raw=true)

- **Apply Patch**: Use Eclipse patch wizard (for diff blocks)

### Review Changes Dialog

The Review Changes feature opens Eclipse's compare editor for selective code application:

![Review Changes Dialog](website/review-changes-dialog.png?raw=true)

### Apply Patch Wizard

For code blocks marked as `diff`, use Eclipse's built-in patch application:

![Apply Patch Wizard](website/apply-patch-wizard.png?raw=true)

## Git Integration

### Team Context Menu

Access Git-specific features through the Team menu:

![Team Context Menu](website/team-context-menu.png?raw=true)

- **Add Staged Diff**: Analyse current project-level staged changes to help review modifications
- **Git Commit Comment**: Automatically generate descriptive commit messages based on staged changes

### Analysing File-Level Git Diffs

The AI can also analsze file-level Git differences using the right-click context menu:

![Git Diff Analysis](website/file-git-diff.png?raw=true)

## Chat Area Context Menu

Right-click in the chat area for additional options:

![Chat Context Menu](website/chat-context-menu.png?raw=true)

- **Copy**: Copy selected text to clipboard
- **Replace Selection**: Replace editor selection with chat text
- **Review Changes**: Compare chat text with editor selection
- **Paste To Message**: Add clipboard content as message
- **Paste As Context**: Add clipboard content as context

## Navigation & Shortcuts

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Enter` | Send message and request response |
| `Shift+Enter` | Insert newline |
| `Ctrl+Enter` | Send message without response |
| `Ctrl+Z` | Undo in text input |
| `Ctrl+Shift+Z` | Redo in text input |

### Mouse Navigation

- **Ctrl+Scroll**: Navigate to top/bottom of conversation
- **Shift+Scroll**: Navigate between messages
- **Shift+Hold**: Highlight current message while scrolling

## Advanced Features

### Reasoning Model Support

Special support for OpenAI's o1 reasoning models with collapsible thinking blocks:

![Thinking Blocks Example](website/thinking-blocks-example.png?raw=true)

### LaTeX Mathematical Content

Render mathematical formulas and equations:

![LaTeX Rendering Example](website/latex-rendering-example.png?raw=true)

### Conversation Management

- Import/Export conversations as JSON
- Export conversations as Markdown

## Customisation

### Prompt Template System

Customise all prompt templates through the preferences:

![Prompt Templates Page](website/prompt-templates-page.png?raw=true)

#### Template Variables

Templates use the [StringTemplate](https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md) syntax with these context variables:

| Variable | Description |
|----------|-------------|
| `<taskname>` | Name of the current prompt task |
| `<usertext>` | User input from the chat interface |
| `<project>` | Current project name |
| `<filename>` | Active file name |
| `<language>` | Programming language of active file |
| `<tag>` | Markdown language tag for syntax highlighting |
| `<warnings>` | Compiler warnings for active file |
| `<errors>` | Compiler errors for active file |
| `<document>` | Full text of active document |
| `<clipboard>` | Current clipboard contents |
| `<selection>` | Currently selected text (or full document if none) |
| `<lines>` | Description of selected line numbers |
| `<documentation>` | Appropriate documentation format for language |
| `<file_diff>` | Git diff for current file |
| `<project_diff>` | Git diff for entire project |

#### Role Switching

Use the special `<<switch-roles>>` tag to create multi-turn conversations:

````
Analyze this code:
```java
<selection>
```
<<switch-roles>>
````

This creates alternating user and assistant messages for complex interactions.

### JSON/TOML Overrides

Configure advanced API parameters using either JSON or TOML syntax:

**JSON Examples:**
```json
"temperature": 0.7, "max_tokens": 2000, "top_p": 0.9
```

**TOML Examples:**
```toml
temperature = 0.7
max_tokens = 2000
reasoning_effort = "high"
```

### UI Preferences

Customize interface settings:

- **Font Sizes**: Adjust chat and notification text size
- **Timeouts**: Configure connection and request timeouts  
- **Streaming**: Enable/disable real-time response streaming
- **Message Types**: Choose between System and Developer messages

## Best Practices

### Effective Prompting

1. **Be Specific**: Provide clear context about what you want to achieve
2. **Use Selections**: Select relevant code portions rather than entire files
3. **Add Context**: Use "Add As Context" for background information
4. **Iterate**: Use "Discuss" for follow-up questions and refinements

### Workflow Integration

1. **Code Review**: Use before committing changes
2. **Learning**: Use "Explain" on unfamiliar code patterns  
3. **Debugging**: Combine with Eclipse's error detection
4. **Documentation**: Generate comments and documentation regularly

### Performance Tips

- Clear conversation history periodically for large projects
- Use streaming responses for better perceived performance
- Configure appropriate timeouts for your network conditions
- Bookmark frequently used API configurations

## Troubleshooting

### Blank/Grey Browser Widget

If the chat area appears blank or grey, set these environment variables before starting Eclipse:

**Linux/macOS:**
```bash
export WEBKIT_DISABLE_COMPOSITING_MODE=1
export WEBKIT_DISABLE_DMABUF_RENDERER=1
./eclipse
```

**Windows:**
```cmd
SET WEBKIT_DISABLE_COMPOSITING_MODE=1
SET WEBKIT_DISABLE_DMABUF_RENDERER=1
start eclipse.exe
```

### Common Issues

- **Connection Errors**: Verify API URL and key in settings
- **Model Not Found**: Ensure model name matches provider's available models
- **Slow Responses**: Increase request timeout in preferences
- **Large Conversations**: Use Clear button periodically for better performance
- **Memory Issues**: Restart Eclipse if conversations become very large
- **SSL/TLS Errors**: Check firewall and proxy settings

## Security Considerations

**Important**: This plugin sends your code to external AI services. Consider the following:

- **Sensitive Code**: Be cautious when analyzing proprietary or sensitive code
- **Local Providers**: Use Ollama or llama.cpp for complete privacy
- **API Keys**: Store API keys securely and rotate them regularly
- **Network**: Use HTTPS endpoints and consider VPN for additional security

## FAQ

**Q: Can I use this plugin offline?**
A: Yes, with local providers like Ollama or llama.cpp.

**Q: How do I switch between different AI models quickly?**
A: Use the bookmarked API settings feature to save and switch between configurations.

**Q: Can I customise the prompts?**
A: Yes, all prompts are fully customisable through the Prompt Templates preferences page.

## Contributing

### Architecture

The plugin follows Eclipse's standard architecture:
- **Main View**: `MainView` and `MainPresenter` handle UI and business logic
- **Browser Integration**: Custom browser functions for code interaction
- **Network Layer**: OpenAI-compatible API client with streaming support
- **Prompt System**: Template-based prompt generation with context injection

### Adding Features

1. **New Prompt Types**: Add to `Prompts` enum and create template file
2. **Browser Functions**: Extend `DisableableBrowserFunction` for new interactions
3. **Context Variables**: Modify `Context` class to add new template variables

## License

MIT License - See [LICENSE](LICENSE) for details.

This project maintains the same license as the original [AssistAI](https://github.com/gradusnikov/eclipse-chatgpt-plugin) project by Wojciech Gradkowski.