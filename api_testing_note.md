🧪 API Testing — Summary Notes
1️⃣ What is API Testing?

API testing = testing the backend by sending HTTP requests and checking responses

No UI, no buttons, no frontend

Tests the core logic of the system

👉 Common tools: PowerShell scripts, Postman, curl, automated tests

2️⃣ Why API Testing is Important

Backend is the brain of the system

UI may hide bugs, API testing does not

Allows testing before frontend exists

Ensures logic is correct and stable

3️⃣ What We Are Testing

We test inputs → outputs

Treat the backend as a black box

We don’t care how it’s implemented

We care if the behavior is correct

4️⃣ Do We Test the Same Input Every Time?

Yes, intentionally

Same input → same output

If output changes → something is broken

This is called deterministic testing

5️⃣ How Do We Decide Test Inputs?

Test inputs are chosen based on:

① Domain knowledge

Real-world rules (physics, energy, comfort)

Example: lower temperature → less energy

② Baseline vs change

Start from a known baseline

Change one variable at a time

③ Boundary cases

Slight decrease

Significant increase

Combined changes

Systems fail at edges, not averages

④ Real decision scenarios

Inputs represent real human decisions

Example: “What if I reduce target temperature by 1°C?”

6️⃣ What Is the Testing Logic?
Define expected behavior
→ Choose input that triggers it
→ Run test
→ Compare output with expectation
→ Fix model logic if violated
→ Repeat

7️⃣ What Are We Actually Checking?

Energy saved > 0 (should save)

Energy saved < 0 (should cost more)

Percent change makes sense

Results are consistent

8️⃣ Why This Is Good Practice

Professional backend practice

Used in real industry systems

Suitable for Digital Twins

Strong evidence of engineering thinking

Very good for FYP / dissertation

9️⃣ Key Takeaway (Important)

We don’t test to find numbers.
We test to enforce real-world rules.