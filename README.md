# Cu

A work-in-progress distributed continuous integration / delivery system.

## Design notes

* Prefer config files over database, so config is in version control.
* Trigger a build from a GitHub receive hook HTTP POST request, or other POST
request with certain payload data.
* Use message queues (currently SQS) to distribute work.
* Commit-centric (git for now).
* A pipeline is a series of commands run against a single version of a
repository.
* If you need to work with multiple repositories, build a parent repo with
submodules.

## Usage

Not yet.

## License

Copyright © 2013 Andrew Bruce

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
