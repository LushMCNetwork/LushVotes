# nuvotifier-velocity-stub-2.6.0.jar

Compile-only stand-in for `com.github.NuVotifier.NuVotifier:nuvotifier-velocity:v2.6.0`
(`provided` scope - never shaded into the plugin jar). LushVotes only touches two members
of that dependency (`VoteListener`): `VotifierEvent.getVote()`, `Vote.getUsername()`,
`Vote.getServiceName()`. This jar declares exactly those two classes/three members with
matching package names and signatures, sourced directly from NuVotifier's real v2.6.0
source (linked in each stub's javadoc) - nothing else, no logic.

## Why not depend on the real artifact

JitPack cannot build NuVotifier at all any more: its root `build.gradle` resolves
`com.jfrog.bintray.gradle:gradle-bintray-plugin` from `jcenter()`, and JCenter/Bintray
was permanently shut down in 2021. Every fresh build (verified by requesting the jar via
the `v2.6.0` tag's exact commit, forcing a non-cached rebuild) fails at Gradle
configuration time before it reaches any subproject:

```
Could not find any matches for com.jfrog.bintray.gradle:gradle-bintray-plugin:1.+
```

A stale build from 2022 is still cached under the `v2.6.0` *tag* coordinate, but per its
own `build.log` (`https://jitpack.io/com/github/NuVotifier/NuVotifier/v2.6.0/build.log`)
it only ever produced `.pom` files for each module, no `.jar` - so it was never actually
usable either. This isn't something a version bump or repository config fixes; the
upstream project's jitpack build is permanently dead.

## Regenerating this jar

```
mkdir -p /tmp/nuvotifier-stub/src/com/vexsoftware/votifier/model
mkdir -p /tmp/nuvotifier-stub/src/com/vexsoftware/votifier/velocity/event
# write Vote.java / VotifierEvent.java (see this repo's git history for the source)
cd /tmp/nuvotifier-stub && javac --release 21 -d classes $(find src -name "*.java")
cd classes && jar cf ../nuvotifier-velocity-stub-2.6.0.jar .
```

`--release 21` is required, not optional. Without it `javac` emits class files for
whatever JDK happens to be installed locally; a JDK newer than 21 produces class files
CI's JDK 21 cannot read at all, failing with `cannot access VotifierEvent` rather than
anything that points at the real cause. Verify with
`unzip -p libs/nuvotifier-velocity-stub-2.6.0.jar 'com/vexsoftware/votifier/velocity/event/VotifierEvent.class' | xxd | head -1`
- bytes 5-8 must read `0000 0041` (65 = Java 21), not higher.

## The better fix, if available

This stub is a last resort, not the ideal dependency - it has no runtime behavior and
will silently drift if NuVotifier's real `VotifierEvent`/`Vote` shape ever changes. If
the actual NuVotifier-Velocity jar installed on the production proxy is available (its
`plugins/` directory, a deploy artifact, etc.), that real jar is strictly better to build
against: drop it in as `libs/nuvotifier-velocity-2.6.0.jar` and repoint the `pom.xml`
`<systemPath>` at it instead.

---

# lushlicense-1.0.0.jar

Vendored copy of `com.playgamesinteractive:lushlicense:1.0.0`, the shared license and
heartbeat client this plugin shades in.

LushMCNetwork/LushLicense is a private repository and the artifact is not published to
any remote Maven repo, so CI cannot resolve it the way a local `mvn install` can. The
workflow installs this copy into the runner's local repository before building. Local
builds keep using whatever is already in `~/.m2` and ignore these files entirely.

## Refreshing after a LushLicense change

```
cd ../LushLicense && mvn install
cp ~/.m2/repository/com/playgamesinteractive/lushlicense/1.0.0/lushlicense-1.0.0.jar libs/
cp ~/.m2/repository/com/playgamesinteractive/lushlicense/1.0.0/lushlicense-1.0.0.pom libs/
```

The jar must be built with Java 21 or lower - CI runs JDK 21 and cannot read class files
from a newer JDK. Verify with
`unzip -p libs/lushlicense-1.0.0.jar 'com/playgamesinteractive/lushlicense/LicenseStateListener.class' | xxd | head -1`;
bytes 5-8 must read `0000 0041` (65 = Java 21) or lower.
