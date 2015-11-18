package io.sniffy.spock

import io.sniffy.Sniffer
import io.sniffy.WrongNumberOfQueriesError
import groovy.sql.Sql
import spock.lang.*

class QueryCounterSpec extends Specification {

    @Shared
    def sql = Sql.newInstance("sniffer:jdbc:h2:mem:", "sa", "sa")

    def spy = Sniffer.spy()

    @FailsWith(WrongNumberOfQueriesError)
    def "Execute single query - negative"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(1)
    }

    def "Execute single query"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(1).reset()

        when:
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(1)
    }

    def "Execute single query - another one"() {
        when:
        sql.execute("SELECT 1 FROM DUAL")

        then:
        spy.verify(1)
    }

}