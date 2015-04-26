package com.github.bedrin.jdbc.sniffer.scalatest

import java.sql.{DriverManager, Statement}

import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError
import org.scalatest._

class QueryCounterSpec extends FunSuite with QueryCounter {

  def connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:s", "sa", "sa")

  test("Sniffer validates the number of queries executed by given code") {
    expect(1) {
      val statement: Statement = connection.createStatement
      statement.execute("SELECT 1 FROM DUAL")
    }
    intercept[WrongNumberOfQueriesError] {
      expectAtMost(2) {
        val statement: Statement = connection.createStatement
        statement.execute("SELECT 1 FROM DUAL")
        statement.execute("SELECT 1 FROM DUAL")
        statement.execute("SELECT 1 FROM DUAL")
      }
    }
  }

}