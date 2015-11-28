package kotliquery

import org.junit.Test
import java.sql.DriverManager
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UsageTest {

    data class Member(
            val id: Int,
            val name: String?,
            val createdAt: Date)

    val toMember: (Row) -> Member = {
        row ->
        Member(row.int("id")!!, row.string("name"), row.sqlTimestamp("created_at")!!)
    }

    val insert = "insert into members (name,  created_at) values (?, ?)"

    @Test
    fun sessionUsage() {

        val conn = DriverManager.getConnection("jdbc:h2:mem:hello", "user", "pass")
        val session = Session(Connection(conn, "org.h2.Driver"))

        session.run(queryOf("drop table members if exists").asExecute)
        session.execute(queryOf("""
create table members (
  id serial not null primary key,
  name varchar(64),
  created_at timestamp not null
)
        """))
        session.update(queryOf(insert, "Alice", Date()))
        session.update(queryOf(insert, "Bob", Date()))

        val ids: List<Int> = session.list(queryOf("select id from members"), { row -> row.int("id") })
        assertEquals(2, ids.size)

        val members: List<Member> = session.list(queryOf("select id, name, created_at from members"), toMember)
        assertEquals(2, members.size)

        var count = 0
        session.forEach(queryOf("select id from members"), { row ->
            count++
            assertNotNull(row.int("id"))
        })
        assertEquals(2, count)

        val nameQuery = "select id, name, created_at from members where name = ?"
        val alice: Member? = session.single(queryOf(nameQuery, "Alice"), toMember)
        assertNotNull(alice)

        val bob: Member? = session.single(queryOf(nameQuery, "Bob"), toMember)
        assertNotNull(bob)

        val chris: Member? = session.single(queryOf(nameQuery, "Chris"), toMember)
        assertNull(chris)
    }

    @Test
    fun actionUsage() {

        val conn = DriverManager.getConnection("jdbc:h2:mem:hello", "user", "pass")
        val session = Session(Connection(conn, "org.h2.Driver"))

        session.run(queryOf("drop table members if exists").asExecute)
        session.run(queryOf("""
create table members (
  id serial not null primary key,
  name varchar(64),
  created_at timestamp not null
)
        """).asExecute)

        session.run(queryOf(insert, "Alice", Date()).asUpdate)
        session.run(queryOf(insert, "Bob", Date()).asUpdate)

        val ids: List<Int> = session.run(queryOf("select id from members").map { row -> row.int("id") }.asList)
        assertEquals(2, ids.size)

        val members: List<Member> = session.run(queryOf("select id, name, created_at from members").map(toMember).asList)
        assertEquals(2, members.size)

        var count = 0
        session.forEach(queryOf("select id from members"), { row ->
            count++
            assertNotNull(row.int("id"))
        })
        assertEquals(2, count)

        val nameQuery = "select id, name, created_at from members where name = ?"
        val alice: Member? = session.run(queryOf(nameQuery, "Alice").map(toMember).asSingle)
        assertNotNull(alice)

        val bob: Member? = session.run(queryOf(nameQuery, "Bob").map(toMember).asSingle)
        assertNotNull(bob)

        val chris: Member? = session.run(queryOf(nameQuery, "Chris").map(toMember).asSingle)
        assertNull(chris)
    }

}