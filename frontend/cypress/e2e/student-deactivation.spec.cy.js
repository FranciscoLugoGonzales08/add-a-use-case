const student = (enabled) => ({
  id: 5,
  username: 'e.hudson@abc.edu',
  enabled,
  firstName: 'Eric',
  lastName: 'Hudson',
  email: 'e.hudson@abc.edu',
  teamName: 'Team1'
})

const response = (data) => ({
  flag: true,
  code: 200,
  message: 'Success',
  data
})

describe('Student deactivation and reactivation', () => {
  beforeEach(() => {
    cy.login('b.wei@abc.edu', '123456')
  })

  function openStudent(studentData) {
    cy.intercept('POST', '**/students/search*', {
      body: response({
        content: [studentData],
        number: 0,
        size: 50,
        totalElements: 1
      })
    }).as('searchStudents')
    cy.intercept('GET', '**/students/5', { body: response(studentData) }).as('findStudent')

    cy.visit('/students')
    cy.wait('@searchStudents')
    cy.contains('tr', studentData.firstName)
      .find('[data-cy=edit-student]')
      .click()
    cy.wait('@findStudent')
  }

  it('cancels the deactivation warning without sending a request', () => {
    openStudent(student(true))
    cy.intercept('POST', '**/students/5/deactivate').as('deactivateStudent')

    cy.get('[data-cy=student-deactivate]').click()
    cy.get('.el-message-box').contains('Cancel').click()

    cy.get('.el-message-box').should('not.exist')
    cy.get('@deactivateStudent.all').should('have.length', 0)
    cy.contains('.el-dialog', 'Enabled').should('be.visible')
  })

  it('deactivates the student only after confirmation', () => {
    openStudent(student(true))
    cy.intercept('POST', '**/students/5/deactivate', {
      body: response(student(false))
    }).as('deactivateStudent')

    cy.get('[data-cy=student-deactivate]').click()
    cy.get('.el-message-box').contains('Deactivate').click()

    cy.wait('@deactivateStudent').then(({ request }) => {
      expect(request.body).to.be.oneOf([undefined, null, ''])
    })
    cy.contains('.el-dialog', 'Disabled').should('be.visible')
  })

  it('reactivates a disabled student only after confirmation', () => {
    openStudent(student(false))
    cy.intercept('PUT', '**/students/5', {
      body: response(student(true))
    }).as('reactivateStudent')

    cy.get('[data-cy=student-reactivate]').click()
    cy.get('.el-message-box').contains('Reactivate').click()

    cy.wait('@reactivateStudent').its('request.body.enabled').should('be.true')
    cy.contains('.el-dialog', 'Enabled').should('be.visible')
  })
})
