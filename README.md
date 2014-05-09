## Synopsis

This project is an implementation of an OpenJPA plugin for distributed caching
with a Redis backend. I've implemented both a DataCache and a QueryCache
plugin.

Keep in mind that using these plugins means that the underlying database must
only be written through a data model that uses this cache; otherwise you must
flush the Redis cache if you write the database independently.

## Code Example

TBD

## Installation

I haven't deployed to Maven yet due to lack of tests, but you may clone the
github repository and then run `mvn install` from the root directory.

## API Reference

Once you've run `mvn install` above, you may include the plugin in your persistence.xml as follows:
```xml
<persistence>
	<persistence-unit name="persistence-unit-name" transaction-type="JTA">
		<properties>
			<property name="openjpa.DataCache" value="com.github.jzila.cache.RedisDataCache"/>
			<property name="openjpa.QueryCache" value="com.github.jzila.cache.RedisQueryCache"/>
			<property name="openjpa.RemoteCommitProvider" value="sjvm"/>
		</properties>
	</persistence-unit>
</persistence>
```

You must also create a file named openjpa\_redis.xml in your classpath. If you
do not do this, the defaults will apply (localhost:6379 as the only server, and
an empty prefix). This file should appear as follows (placeholders for
localhost):

```xml
<redis>
	<!-- The list of servers that will be used for the distributed cache. Uses
		ShardedJedis to perform the consistent hashing -->
    <servers>
        <server>
            <host>localhost</host>
            <port>6379</port>
        </server>
    </servers>
	<!-- The prefix that will be used to preface the keys in your Redis databse -->
    <prefix>foo</prefix>
</redis>
```

## Tests

TBD

## Contributors

This project is entirely hosted on GitHub. If you wish to file an issue, you may do so by submitting a GitHub issue against this project. You may also submit pull requests from your own fork.

## License

Copyright 2014 John Zila

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
