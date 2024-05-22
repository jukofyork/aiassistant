# TODO:

Follow these steps when tasked with writing code:
1. Understand the problem: constraints, specifications, objective, and edge cases.
2. Create a high-level plan for the solution.
3. Break down the problem into sub tasks.
4. Explain your thought process with justifications.
5. Combine sub task solutions for the main task.
6. Write code to complete the task.


Follow these steps when tasked with writing code:
1. Understand the problem: constraints, specifications, objective, and edge cases.
2. Create a high-level plan for the solution.
3. Break down the problem into sub tasks.
4. Describe your plan for what to build in pseudocode, written out in great detail.
5. Combine sub task solutions for the main task.
6. Write code to complete the task.


Follow these steps when tasked with writing code:
1. Understand the problem: constraints, specifications, objective, and edge cases.
2. Create a high-level plan for the solution.
3. Break down the problem into sub tasks.
4. Describe your plan for what to build in pseudocode, written out in great detail.
5. Combine sub task solutions for the main task.
6. Output the code in a single code block.

Think step-by-step: always describe your plan in great detail before proceeding to the solution.

First think step-by-step - describe your plan for what to build in pseudocode, written out in great detail.
Then output the code in a single code block.
Minimize any other prose.


Effective Prompts Discovered by Experts and Algorithms Explanation

1. “The answer is:” direct prompting
2. “Let’s think step by step:” zero-shot CoT (Kojima et al., 2022)
3. “Let’s work this out in a step by step way to be sure we have the right answer:” APE discovered (Zhou et al., 2022b)
4. “First, decompose the question into several sub-questions that need to be solved, and then solve each question step by step:” Least-to-most (Zhou et al., 2022a)
5. “Imagine three different experts are answering this question. All experts will write down 1 step of their thinking, and then share it with the group. Then all experts will go on to the next step, etc. If any expert realizes they’re wrong at any point then they leave.” Tree-of-thought (Hulbert, 2023)
6. “3 experts are discussing the question, trying to solve it step by step, and make sure the result is correct:” multi-agent debate (Liang et al., 2023

---

Our analysis indicates that the implementation of Chain-of-Thought (CoT) prompting no-
tably enhances the capabilities of DeepSeek-Coder-Instruct models. This improvement becomes
particularly evident in the more challenging subsets of tasks. By adding the directive, "You
need first to write a step-by-step outline and then write the code." following the initial prompt,
we have observed enhancements in performance. This observation leads us to believe that the
process of first crafting detailed code descriptions assists the model in more effectively under-
standing and addressing the intricacies of logic and dependencies in coding tasks, particularly
those of higher complexity. Therefore, we strongly recommend employing CoT prompting strate-
gies when utilizing DeepSeek-Coder-Instruct models for complex coding challenges. Such an
approach promotes a more methodical and logical framework for problem-solving, potentially
resulting in more precise and efficient outcomes in code generation tasks.