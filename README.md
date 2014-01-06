# Cu

A work-in-progress distributed continuous integration / delivery system.

## Design notes

* Prefer config files over database, so config is in version control.
* Trigger a build from a GitHub receive hook HTTP POST request, or other POST
request with certain payload data.
* Use message queues (currently SQS) to distribute work.
* Commit-centric (git for now).

## Usage (proposed)

Create a git repository. You can use the same repository as your project, but 
if you need to test or deploy multiple projects, add them as submodules to a
parent project (this can be automated with a help script in future).

Create a configuration file called cu.yml at the root of the repository. See
example in this repo.

---

Push to web app causes fetch from repo - config is read.

Queue items:

Items stay in waiting queue until a flag on s3 is present that indicates ready
to move to immediate queue e.g.

job1 - job2 (parallel)
job3 (next)

All jobs added to waiting queue when a push is received.
Jobs are enqueued including data about the commit that was pushed.
Jobs with same repo use same commit.

waiting queue runner:
gets job1
reads s3 config - sees that it needs job1 and job2 to be green
enqueues both job1 and job2 to immediate

if there's a job1-green and a job2-green file, enqueue job3 to immediate

## License

Copyright © 2014 Andrew Bruce

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
