from pathlib import Path
import os
import subprocess



if not Path("project").exists():
    print("Run bootstrap")
    cwd = Path(__file__).parents[0]
    repo = "ssh://git@czgitlab.llnl.gov:7999/rda/builder.git"
    run = ["git","clone", repo,"-b","master","project"]
    rc = subprocess.run(run, cwd=cwd)
    if rc.returncode != 0:
        raise RuntimeError("Bootstrap failed")
    print("Bootstrap completed\n")
else:
    # Do a pull 
    print("Checking project directory for updates")
    projectPath = os.path.join(Path(__file__).parents[0], "project")
    rc = subprocess.run("git pull", cwd=projectPath, shell=True)
    if rc.returncode != 0:
        raise RuntimeError("Pull error, bootstrap failed")
    print("Checking for updates completed\n")
