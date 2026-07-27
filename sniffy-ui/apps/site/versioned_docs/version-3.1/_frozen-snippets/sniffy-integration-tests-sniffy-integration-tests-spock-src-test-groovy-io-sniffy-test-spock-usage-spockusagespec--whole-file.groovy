package io.sniffy.test.spock.usage

import groovy.sql.Sql
import io.sniffy.Sniffy
import io.sniffy.sql.SqlQueries
import io.sniffy.sql.WrongNumberOfQueriesError
import spock.lang.FailsWith
import spock.lang.Shared
import spock.lang.Specification

class SpockUsageSpec extends Specification {

    @Shared sql = Sql.newInstance("sniffy:jdbc:h2:mem:", "sa", "sa")

    def spy = Sniffy.spy()

    @FailsWith(WrongNumberOfQueriesError)
    "Execute single query - negative"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(SqlQueries.exactQueries(1))
    }

    def "Execute single query"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(SqlQueries.exactQueries(1)).reset()

        when:
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(SqlQueries.exactQueries(1))
    }

    def "Execute single query - another one"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(SqlQueries.exactQueries(1))
    }

}