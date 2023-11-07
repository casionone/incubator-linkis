# Update a submodule to the latest commit 

Since git 1.8 you can do
```
git submodule update --remote --merge
```
This will update the submodule to the latest remote commit. You will then need to add and commit the change so the gitlink in the parent repository is updated:

First, git add it
```
git add project/submodule_proj_name
```
then git commit it
```
git commit -m 'gitlink to submodule_proj_name was updated'
```
the git push it
```
git push
```

more detail can refer：https://stackoverflow.com/questions/8191299/update-a-submodule-to-the-latest-commit
