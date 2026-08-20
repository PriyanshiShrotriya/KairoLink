#History of work done
#update it regularly

- 2026-08-20 (a): Confirmed tech stack stays Java 17 + Spring Boot + Spring MVC + JSP/JSTL (no change at that point — this was already locked in). Updated `architecture.md`, `rules.md`, and `README.md` to reflect that the project will be vibe-coded in **VS Code using GitHub Copilot / Copilot Chat** instead of IntelliJ: added recommended VS Code extensions, a Copilot-specific workflow/guardrails section in `rules.md` (context-referencing docs, being extra cautious with inline ghost-text suggestions, reviewing layer-by-layer), and VS Code run instructions in `README.md`.

- 2026-08-20 (b): **Switched the view layer from JSP/JSTL to Thymeleaf, and packaging from WAR to JAR**, since the goal is now a real, deployable project rather than a JSP/Servlet learning exercise. Changed across all docs:
  - `architecture.md` — view layer, packaging, folder structure (`src/main/resources/templates/` + `static/` replacing `webapp/WEB-INF/views/`), tech stack tables, added a "Note on Thymeleaf vs JSP" section explaining the rationale, added Docker/Render/Railway/Fly.io as deployment options.
  - `rules.md` — locked stack now says Thymeleaf (not JSP), flipped the "no Thymeleaf" restriction to "no JSP", updated Copilot guardrails to flag JSP/JSTL drift instead.
  - `README.md` — tech stack table, architecture diagram, project structure, prerequisites (dropped hard Apache Tomcat requirement), run instructions (`java -jar ...`), added a "Deploying" section.
  - `phases.md` — Phase 0 (Thymeleaf template + DevTools setup instead of JSP config), Phase 6 (templates instead of JSPs), Phase 8 (JAR + Docker + Render/Railway/Fly.io deployment instead of WAR + external Tomcat).
  - `design.md` — replaced JSP references with Thymeleaf in usage-principle notes and font-loading instructions.
  - `requirements.md` — constraints section updated to say Thymeleaf instead of JSP/Servlets.
  - Everything else (Spring Boot, Spring MVC, Spring Data JPA, Spring Security, Maven, MySQL/PostgreSQL, Bootstrap 5, vanilla JS, layered architecture, entity/DTO structure) is unchanged.