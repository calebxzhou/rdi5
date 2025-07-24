import calebxzhou.rdi.ui2.frag.DialogFragment
import icyllis.modernui.ModernUI
import org.junit.jupiter.api.Test

class UiTest {
    @Test
    fun alert(){
        ModernUI().use { app ->
            app.run(DialogFragment("test", onYes = {
                println("yes")
            }, onNo = {
                println("no")
            }))
        }
    }
    @Test
    fun bg(){

    }
    @Test
    fun test(){
        /*ModernUI().use { app ->
            app.run(TestFragment())
        }*/
    }
}