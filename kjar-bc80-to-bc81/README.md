# Migration A — BC 8.0 KJAR → Business Central 8.1

Take a KJAR **authored in Business Central 8.0** and import it into **Business Central 8.1**.

---

## What is in this folder

```
kjar/          BAMOE 8.0 KJAR source (packaging=kjar, KIE 7.67.x)
```

Assets: `CanDrive.dmn`, `AgeRule.drl`, `HelloProcess.bpmn2`, `AgeScorecard.pmml`, `Applicant.java`, `CanDrive.scesim`, `AgeRule.scesim`

---

## Prerequisites

| Requirement          | Version |
|----------------------|---------|
| JDK                  | 11      |
| Maven                | 3.6+    |
| Business Central 8.1 | running |

---

## Step 1 — Get the KJAR from BC 8.0

In BC 8.0, open the project and click **Build → Build & Download**.

---

## Method A — Import via Git

Create a new empty repo on GitHub, then:

```bash
cd /Users/athirac/Downloads/project   # root of the downloaded project (where pom.xml lives)
git init
git add .
git commit -m "BAMOE 8.0 KJAR"
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin master
```

Then in BC 8.1: **Design → Import Project**, paste `https://github.com/<your-username>/<your-repo>.git`, select the project, click **OK**.

Once imported, click **Build → Build & Deploy** — wait for `Build Successful`.

To run scesim tests: open `CanDrive.scesim` or `AgeRule.scesim` and click **▶ Run**.

---

## Method B — Upload via Artifacts UI 

> Scesim files inside a binary upload are not runnable from the BC UI. Use Method A if you need to run them.

In BC 8.1: **⚙ → Artifacts → Upload**, select the downloaded JAR.

Then **Deploy → Execution Servers → Add Container**, fill in `com.example` / `example-kjar` / `1.0.0`, click **Finish → Deploy**.

> Requires KIE Server in managed mode (`org.kie.server.controller` pointing at BC). Without it, BC shows "No Remote Servers".

---