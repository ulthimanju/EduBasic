# Copilot Instructions

## Code Quality & Style

### General Principles
- Write clean, modular, and maintainable code with single-responsibility functions
- Prefer composition over inheritance where applicable
- Keep functions/methods concise (aim for <30 lines)
- Avoid magic numbers; use named constants instead

### Naming Conventions
- **Variables/Functions:** Descriptive camelCase (e. g., `getUserProfile`, `isValidEmail`)
- **Classes/Components:** PascalCase (e.g., `UserService`, `PaymentModal`)
- **Constants:** SCREAMING_SNAKE_CASE (e. g., `MAX_RETRY_COUNT`, `API_BASE_URL`)
- **Booleans:** Prefix with `is`, `has`, `should`, or `can` (e.g., `isLoading`, `hasAccess`)

### Comments & Documentation
- Explain the "why" for complex or non-obvious logic
- Skip comments for self-explanatory code
- Use JSDoc/Javadoc for public APIs and exported functions
- Document assumptions and edge cases in complex algorithms

## Reliability & Error Handling

### Imports
- Use explicit imports; avoid wildcards (e.g., `import java.util. List` not `import java. util.*`)
- Group and organize imports logically (standard library, third-party, local)

### Error Handling
- Always handle errors for network requests, I/O, and async operations
- Provide meaningful error messages for debugging
- Use try-catch blocks with specific exception types where possible
- Implement graceful degradation for non-critical failures

### Defensive Programming
- Validate inputs at function boundaries
- Handle edge cases: nulls, undefined, empty arrays/strings, zero values
- Use optional chaining and nullish coalescing where supported
- Fail fast with clear error messages for invalid states

## UI & Frontend

### Consistency
- Prioritize predefined design system components over custom CSS
- Maintain strict theme consistency (colors, spacing, typography)
- Use CSS variables or theme tokens for styling values
- Follow responsive design principles

### Accessibility
- Include appropriate ARIA labels and roles
- Ensure keyboard navigation support
- Maintain sufficient color contrast ratios

## Constraints

### Avoid
- Deprecated APIs, libraries, or legacy syntax
- Hardcoded secrets, credentials, or environment-specific values
- Unnecessary dependencies or over-engineering
- Mutating function parameters directly

### Prefer
- Immutable data patterns where practical
- Built-in language features over external libraries for simple tasks
- Early returns to reduce nesting
- Async/await over raw promises or callbacks