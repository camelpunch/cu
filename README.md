# Cu

A work-in-progress distributed continuous integration / delivery system.

Features are pretty sparse at present, and it's not yet ready for your
complicated deployment pipeline.

See the [public Pivotal Tracker project](https://www.pivotaltracker.com/s/projects/981848)
for completed and proposed features.

File GitHub Issues for feature proposals or bug reports.

## Design notes

* Prefer config files over database, so config is in version control.
* Few dependencies, workers deployable with jars.
* Trigger a pipeline run from a [GitHub receive hook HTTP POST request](https://help.github.com/articles/post-receive-hooks#the-payload),
or other POST request with certain payload data.
* Use message queues (currently SQS) to distribute work.
* Parallel pipeline steps.
Can run parallel builds and depend on them all being green before proceeding.

## Prerequisites

You currently need an AWS account.
Future support for other services is anticipated.

I suggest you create an IAM user with permissions to do everything on SQS and
S3 (these are the only required services).
You'll need the IAM user's access key and secret key.

Alternatively, you can use a global account access key and secret.

Create a bucket that the IAM user can access and use it in your cu.yml

Cu will try to create cu-pushes and cu-builds SQS queues.

## Usage instructions for trying Cu locally

Install [leiningen](http://leiningen.org) first.
This installs Clojure.
You won't need to install Clojure on deployed systems, but you will need a JRE.

Mac:

```shell
brew install leiningen
```

Debian / Ubuntu:

```shell
apt-get install leiningen
```

Clone this repository.

Modify cu.yml at the root of the repository to suit your needs.

Add the following to ~/.lein/profiles.clj.

```clojure
{:user {:env {:aws-access-key "YOURACCESSKEY"
              :aws-secret-key "YOURSECRETKEY"
              :cu-username "yourwebusername"
              :cu-password "yourwebpassword"}}}
```

Start the web app, which receives pushes and shows logs etc:

```shell
PORT=3000 lein run -m cu.web
```

In another shell, start a parser, which parses web pushes and queues them up
for workers:

```shell
lein run parser
```

In one or more other shells, start a worker, which runs builds:

```shell
lein run worker
```

Create a fake payload file (to pretend GitHub has pushed):

```shell
echo 'payload={"repository":{"name":"my-repo","url":"file:///path/to/cloned/cu"}}' > payload.txt
```

Trigger a fake push request:

```shell
curl -XPOST http://yourwebusername:yourwebpassword@localhost:3000/push -d@payload.txt
```

You should see your build(s) get triggered in the order you specified.
If you ran multiple workers, they might have run in parallel.

## License

Copyright © 2014 Andrew Bruce

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
