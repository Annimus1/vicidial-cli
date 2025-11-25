package dev.pablo.api;

import java.io.IOException;
import java.lang.InterruptedException;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.Ansi;

@Command(
    name="UpdateCred",
    description="Actualiza las credenciales de un Usuario y un Phone a la vez.",
    mixinStandardHelpOptions=true
)
public class UpdateCredCommand implements Callable<Integer> {

    private VicidialClientSingleton client;
    @Parameters(index = "0", description = "Identificador de la credencial.")
    private String ID;
    @Option(names = {"-n", "--name"}, description = "Nombre que tendra el usuario actualizado.", defaultValue="")
    private String name;
    @Option(names = {"-p", "--password"}, description = "Nueva Password para el Usuario y el Phone.", defaultValue="")
    private String password;

    public UpdateCredCommand() {
        this.client = VicidialClientSingleton.getInstance();
    }

    public String getID(){
        return this.ID;
    }

    public void setID(String ID){
        this.ID = ID;
    }

    @Override
    public Integer call() throws Exception {
        System.out.println(Ansi.AUTO.text("⏱️  @|yellow Updating Credentials for: " + ID + "...|@"));
        
        if(password.isEmpty() && name.isEmpty()){
            System.err.println(Ansi.AUTO.text("❌ @|red In order to update a credencial need to provide either name, password or both.|@"));
            return 1;
        }
        
        try{
            System.out.println(Ansi.AUTO.text("@|blue Updating User ...|@"));
            client.updateUser(ID, name, password);

            System.err.println(Ansi.AUTO.text("☑️ @|blue Name and password has been updated.|@"));
            
            if(!password.isEmpty()){
                System.out.println(Ansi.AUTO.text("@|blue Updating Phone ...|@"));
                client.updatePhone(ID, password);
                System.err.println(Ansi.AUTO.text("☑️ @|blue Phone's password has been updated.|@"));
            }
            System.out.println(Ansi.AUTO.text("✅ @|green The credentials have been successfully updated.|@"));
        }
        catch(IOException e){
            System.err.println(Ansi.AUTO.text("❌ @|red Error de la API o de red: |@ " + e.getMessage()));
            return 1;
        }
        catch(InterruptedException e ){
            System.err.println(Ansi.AUTO.text("❌ @|red La petición fue interrumpida.|@"));
            Thread.currentThread().interrupt(); 
            return 1;
        } 
        return null;
    }

}
