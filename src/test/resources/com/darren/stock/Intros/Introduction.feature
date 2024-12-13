@section-Intro

  Feature: Introduction

    [ditaa, format="svg"]
    -----
    +--------+   +-------+    +-------+
    |        | --+ ditaa +--> |       |
    |  Text  |   +-------+    |diagram|
    |Document|   |!magic!|    |       |
    |     {d}|   |       |    |       |
    +---+----+   +-------+    +-------+
        :                         ^
        |       Lots of work      |
        +-------------------------+
    -----

    [a2s, format="svg"]
    -----
    .-------------.  .--------------.
    |[Red Box]    |  |[Blue Box]    |
    '-------------'  '--------------'

    [Red Box]: {"fill":"#aa4444"}
    [Blue Box]: {"fill":"#ccccff"}
    -----

    [graphviz, format="svg"]
    -----
    digraph { a -> b }
    -----

    [mermaid]
    -----
    %%{init: {'theme':'neutral'}}%%
    sequenceDiagram
      Alice->>John: Hello John, how are you?
      John-->>Alice: Great!
      Alice-)John: See you later!
    -----

    [mermaid]
    -----
    %%{init: {'theme':'neutral'}}%%
    pie title Pets adopted by volunteers
      "Dogs" : 386
      "Cats" : 85
      "Rats" : 15
    -----

    Scenario: Root