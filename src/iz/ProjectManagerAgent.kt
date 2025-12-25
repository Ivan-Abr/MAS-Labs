package iz

import jade.core.Agent
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException

class ProjectManagerAgent : Agent() {
    private var gui: ProjectManagerGui? = null
    private var programmersCount = 0
    private var programmerSkill = 0
    private var projectValue = 0

    override fun setup() {
        gui = ProjectManagerGui(this)
        gui!!.show()
        val dfd = DFAgentDescription()
        dfd.name = aid
        val sd = ServiceDescription()
        sd.type = "project-hiring"
        sd.name = "JADE-book-hiring"
        dfd.addServices(sd)
        try {
            DFService.register(this, dfd)
        } catch (fe: FIPAException)  {
            fe.printStackTrace()
        }
    }

    override fun takeDown() {
        try {
            DFService.deregister(this)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
        gui!!.dispose()
        println("Project manager " + aid.name + " terminating.")
    }

//    fun updateCatalogue(title: String?, price : Int) {
//        addBehaviour(object : OneShotBehaviour() {
//            override fun action() {
//
//            }
//        })
//    }
}