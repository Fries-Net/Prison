/*
 *  Prison is a Minecraft plugin for the prison game mode.
 *  Copyright (C) 2017-2020 The Prison Team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.mcprison.prison.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.mcprison.prison.Prison;
import tech.mcprison.prison.internal.CommandSender;
import tech.mcprison.prison.output.ChatDisplay;
import tech.mcprison.prison.output.LogLevel;
import tech.mcprison.prison.output.Output;
import tech.mcprison.prison.output.Output.DebugTarget;


public class RegisteredCommand
		implements Comparable<RegisteredCommand>  {

    private String label;
    private CommandHandler handler;
    private RegisteredCommand parent;
    private boolean alias = false;
    
    private int usageCount;
    private long usageRunTimeNanos;
    
    private String junitTest = null;
    
    private String description;
    private String[] permissions;
    private String[] altPermissions;
    private String[] aliases;
    private String[] docURLs;
    
    
    private List<RegisteredCommand> registeredAliases;
    private RegisteredCommand parentOfAlias;
    
    private boolean onlyPlayers;
    private Method method;
    private Object methodInstance;

    private boolean set = false;

    private ArrayList<ExecutableArgument> methodArguments = new ArrayList<ExecutableArgument>();
    private ArrayList<CommandArgument> arguments = new ArrayList<CommandArgument>();

    private WildcardArgument wildcard;
    
    private ArrayList<Flag> flags = new ArrayList<Flag>();
    private Map<String, Flag> flagsByName = new LinkedHashMap<String, Flag>();

    private ArrayList<RegisteredCommand> suffixes = new ArrayList<RegisteredCommand>();
    private Map<String, RegisteredCommand> suffixesByName = new HashMap<String, RegisteredCommand>();

    
    public RegisteredCommand(String label, CommandHandler handler, RegisteredCommand parent) {
        this.label = label;
        this.handler = handler;
        this.parent = parent;
        
        this.registeredAliases = new ArrayList<>();
        
        this.usageCount = 0;
        this.usageRunTimeNanos = 0;
    }

    /**
     * For JUnit testing ONLY!  Never use for anything else!
     */
    private RegisteredCommand( String jUnitUsage ) {
    	
    	this.junitTest = jUnitUsage;
    	
    	this.label = "junitTest";
    	this.handler = null;
    	this.parent = null;
    	
    	this.registeredAliases = new ArrayList<>();
    }
    
    protected static RegisteredCommand junitTest( String jUnitUsage ) {
    	RegisteredCommand results = new RegisteredCommand( jUnitUsage );
    	
    	return results;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append( getUsage() )
    			.append( "  isRoot: " ).append( isRoot() )
    			.append( "  isAlias: " ).append( isAlias() )
    			.append( "  suffixCnt: " ).append( getSuffixes().size() )
    			.append( "  hasAliasParent: " ).append( getParentOfAlias() != null );
    	
    	if ( getParentOfAlias() != null ) {
    		sb.append( " (" ).append( getParentOfAlias().getUsage() ).append( ")" );
    	}
    	
    	return sb.toString();
    }
    
    /**
     * The suffix is converted to all lowercase before adding to the map.
     *  
     * @param suffix
     * @param command
     */
    void addSuffixCommand(String suffix, RegisteredCommand command) {
        suffixesByName.put( suffix.toLowerCase(), command);
        suffixes.add(command);
    }
    
    /**
     * The suffix is converted to all lowercase before checking to see if it exists in the map.
     * 
     * @param suffix
     * @return if the suffix exists
     */
    boolean doesSuffixCommandExist(String suffix) {
        return suffixesByName.containsKey( suffix.toLowerCase() );
    }
    
    public String getCompleteLabel() {
    	return (parent == null ? "" : parent.getCompleteLabel() + " " ) + 
    			(label == null ? "-noCommandLabelDefined-" : label) ;
    }

    /**
     * <p>This will take the args list and try to find the command that was registered
     * with this command handler.  What should be noted is that if the given arguments
     * could not be resolved, then it strips off the first arg, then recursively 
     * tries again. 
     * </p>
     * 
     * @param sender
     * @param args
     */
    protected void execute(CommandSender sender, String[] args) {
    	execute( sender, args, true );
    }
	protected void execute(CommandSender sender, String[] args, boolean allowDebug ) {
    	
    	// Only enable debug mode if the selective debug target is commandHanlder.
    	// To enable this use ...
    	boolean debug = Output.get().isActiveTarget( DebugTarget.commandHandler );
    	
    	// First ensure the player is not locked out of this command:
    	if ( !handler.hasCommandAccess(sender, this, getLabel(), args) ) {
    		
    		if ( debug ) {
    			String argz = "";
    			for (String arg : args) {
    				argz += arg + " ";
    			}
    			
    			String msg = String.format( 
    					"RegisteredCommand.execute: Player %s does not have access to a command: [%s]",
    					sender.getName(), 
    					(getLabel() + " " + argz).trim()
    					);
    			Output.get().logInfo( msg );
    		}
    		return;
    	}
    	
    	if ( debug ) {
    		String argz = "";
    		for (String arg : args) {
    			argz += arg + " ";
    		}
    		
    		String msg = String.format( 
    				"RegisteredCommand.execute: Player %s is attempting to run command: [%s]",
    				sender.getName(), 
    				(getLabel() + " " + argz).trim()
    				);
    		Output.get().logInfo( msg );
    	}
    	
        if (!testPermission(sender)) {
            Prison.get().getLocaleManager().getLocalizable("noPermission")
                .sendTo(sender, LogLevel.ERROR);
            
            Output.get().logInfo( "&cLack of Permission Error: &7Player &3%s &7lacks permission to " +
            		"run the command &3%s&7. Permissions needed: [&3%s&7]. Alt Permissions: [&3%s&7]", 
            			sender.getName(), getCompleteLabel(),
            			(permissions == null ? "-none-" : String.join( ", ", permissions )),
            			(altPermissions == null ? "-none-" : String.join( ", ", altPermissions ))
            		);
            return;
        }

        if (args.length > 0) {
            String suffixLabel = args[0].toLowerCase();
            if (suffixLabel.equals( CommandHandler.COMMAND_HELP_TEXT )) {
                sendHelpMessage(sender);
                return;
            }

            RegisteredCommand command = suffixesByName.get(suffixLabel);
            if (command == null) {
                
//                Output.get().logError( "### #### RegisteredCommands.execute : 1  " +
//                		"if(command == null) ::  args.length = " + 
//                			(args == null ? "null" : args.length) +
//                			"  args[0] == " + args[0]);

            	// all checks were passed, and this is where the 
            	// command is executed...
                executeMethod( sender, args );
            } 
            else {
            	// Strip first arg, then recursively try again
                String[] nargs = new String[args.length - 1];
                System.arraycopy(args, 1, nargs, 0, args.length - 1);
                command.execute( sender, nargs, false );
            }
        } 
        else {
        	// all checks were passed, and this is where the 
        	// command is executed...
            executeMethod( sender, args );
        }

    }

    private void executeMethod(CommandSender sender, String[] args ) {
        if (!set) {
            sendHelpMessage(sender);
            return;
        }

        ArrayList<Object> resultArgs = new ArrayList<Object>();
        resultArgs.add(sender);

        long nanosStart = 0;
        long nanosEnd = 0;
        
        Arguments arguments;
        try {
            arguments = new Arguments(args, flagsByName);
        } catch (CommandError e) {
            Output.get().sendError(sender, e.getColorizedMessage());
            return;
        }

        for (ExecutableArgument ea : this.methodArguments) {
            try {
                resultArgs.add(ea.execute(sender, arguments));
            } catch (CommandError e) {
                Output.get().sendError(sender, e.getColorizedMessage());
                if (e.showUsage()) {
                    sender.sendMessage(getUsage());
                }
                return;
            }
        }

        try {
            try {
            	// The command is ran here with the invoke...
            	
            	// Record that the command has been "ran", which does not mean it was successful:
            	incrementUsageCount();
            	nanosStart = System.nanoTime();
            	
                method.invoke(getMethodInstance(), resultArgs.toArray());
                
                nanosEnd = System.nanoTime();
                
                long nanosDuration = nanosEnd - nanosStart;
                this.usageRunTimeNanos += nanosDuration;
                
            } 
            catch ( IllegalArgumentException | InvocationTargetException e) {

            	nanosEnd = System.nanoTime();
                
            	long nanosDuration = nanosEnd - nanosStart;
            	this.usageRunTimeNanos += nanosDuration;
            	
            	if (e.getCause() instanceof CommandError) {
                    CommandError ce = (CommandError) e.getCause();
                    Output.get().sendError(sender, ce.getColorizedMessage());
                    if (ce.showUsage()) {
                        sender.sendMessage(getUsage());
                    }
                } 
                else {
    				StringBuilder sb = new StringBuilder();
    				
    				for ( Object arg : resultArgs ) {
    					sb.append( "[" );
    					sb.append( arg.toString() );
    					sb.append( "] " );
    				}

                	String message = "RegisteredCommand.executeMethod(): Invoke error: [" +
                				e.getMessage() + "] cause: [" +
                				(e.getCause() == null ? "" : e.getCause().getMessage()) + "] " + 
                				" target instance: [methodName= " +
                				method.getName() + "  parmCnt=" + method.getParameterCount() + "  methodInstance=" + 
                				getMethodInstance().getClass().getCanonicalName() + "] " +
                				"command arguments: " + sb.toString()
                				;

                	// Warning: if the args contains a % then the following sendError will fail because 
                	//          the % will be treated as String.format() placeholders.  So to be safe and
                	//          to prevent this failure, escape all % with a double % such as %%.
                	message = message.replace( "%", "%%" );
                	
                	Output.get().logError( message );
                	
                	Output.get().sendError( sender, "An exception has occurred. Details have been " +
                			"logged to the server's console." );

                	// Generally these errors are major and require program fixes, so throw
                	// the exception so the stacklist is logged.
                    throw e;
                }
            }
        } catch (Exception e) {
            Prison.get().getLocaleManager().getLocalizable("internalErrorOccurred")
                .sendTo(sender, LogLevel.ERROR);
            e.printStackTrace();
        }
    }

    private ArgumentHandler<?> getArgumenHandler(Class<?> argumentClass) {
        ArgumentHandler<?> argumentHandler = handler.getArgumentHandler(argumentClass);

        if (argumentHandler == null) {
            throw new RegisterCommandMethodException(method,
                "Could not find a ArgumentHandler for (" + argumentClass.getName() + ")");
        }

        return argumentHandler;
    }
    
    public boolean isRoot() {
    	return (this instanceof RootCommand);
    }

    public List<CommandArgument> getArguments() {
        return arguments;
    }

    public String getDescription() {
        return description;
    }

    public List<Flag> getFlags() {
        return flags;
    }

    public ChatDisplay getHelpMessage( CommandSender sender ) {
        return handler.getHelpHandler().getHelpMessage( sender, this );
    }

    public String getLabel() {
        return label;
    }

    public RegisteredCommand getParent() {
        return parent;
    }

    public boolean isAlias() {
		return alias;
	}
	public void setAlias( boolean alias ) {
		this.alias = alias;
	}

    public String[] getPermissions() {
        return permissions;
    }

    public String[] getAltPermissions() {
		return altPermissions;
	}

    public String[] getAliases() {
		return aliases;
	}

	public String[] getDocURLs() {
		return docURLs;
	}

	public List<RegisteredCommand> getRegisteredAliases() {
		return registeredAliases;
	}
	
	public RegisteredCommand getParentOfAlias() {
		return parentOfAlias;
	}
	public void setParentOfAlias( RegisteredCommand parentOfAlias ) {
		this.parentOfAlias = parentOfAlias;
	}

	private Object getMethodInstance() {
		return methodInstance;
	}

    public RegisteredCommand getSuffixCommand(String suffix) {
        return suffixesByName.get(suffix.toLowerCase());
    }

    public List<RegisteredCommand> getSuffixes() {
        return suffixes;
    }

    public String getUsage() {
        return 
        		junitTest == null ?
        				handler.getHelpHandler().getUsage(this) : 
        				junitTest;
    }

    public WildcardArgument getWildcard() {
        return wildcard;
    }

    public boolean isOnlyPlayers() {
        return onlyPlayers;
    }

    public boolean isSet() {
        return set;
    }

//    public boolean onlyPlayers() {
//        return onlyPlayers;
//    }

    public void sendHelpMessage(CommandSender sender) {
    	
    	ChatDisplay chatDisp = getHelpMessage( sender );
    	
    	if ( chatDisp != null ) {
    		chatDisp.send( sender );
    	}
    }

    void set(Object methodInstance, Method method) {
        this.methodInstance = methodInstance;
        this.method = method;
        method.setAccessible(true);
        Command command = method.getAnnotation(Command.class);
        Flags flagsAnnotation = method.getAnnotation(Flags.class);
        this.description = command.description();
        this.permissions = command.permissions();
        this.altPermissions = command.altPermissions();
        
    	String[] aliases = CommandHandler.addConfigAliases( command.identifier(), command.aliases() );
    			//addConfigAliases( command.identifier(), command.aliases() );
        this.aliases = aliases;
        
//        this.aliases = command.aliases();
        this.docURLs = command.docURLs();
        
        this.onlyPlayers = command.onlyPlayers();

        Class<?>[] methodParameters = method.getParameterTypes();

        if (methodParameters.length == 0 || !CommandSender.class
            .isAssignableFrom(methodParameters[0])) {
            throw new RegisterCommandMethodException(method,
                "The first parameter in the command method must be assignable to the CommandSender interface.");
        }

        if (flagsAnnotation != null) {
            String[] flags = flagsAnnotation.identifier();
            String[] flagdescriptions = flagsAnnotation.description();

            for (int i = 0; i < flags.length; i++) {
                Flag flag =
                    new Flag(flags[i], i < flagdescriptions.length ? flagdescriptions[i] : "");
                this.flagsByName.put(flags[i], flag);
                this.flags.add(flag);
            }
        }

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 1; i < methodParameters.length; i++) {

            //Find the CommandArgument annotation
            Arg commandArgAnnotation = null;
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType() == Arg.class) {
                    commandArgAnnotation = (Arg) annotation;
                }
            }

            //Find the FlagArg annotation
            FlagArg flagArgAnnotation = null;
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType() == FlagArg.class) {
                    flagArgAnnotation = (FlagArg) annotation;
                }
            }

            //If neither does not exist throw
            if (commandArgAnnotation == null && flagArgAnnotation == null) {
                throw new RegisterCommandMethodException(method,
                    "The command annotation is present on a method, however one of the parameters is not annotated.");
            }

            Flag flag = null;

            if (flagArgAnnotation != null) {
                flag = this.flagsByName.get(flagArgAnnotation.value());
                if (flag == null) {
                    throw new RegisterCommandMethodException(method,
                        "The flag annotation is present on a parameter, however the flag is not defined in the flags annotation.");
                }
            }

            Class<?> argumentClass = methodParameters[i];

            if (commandArgAnnotation == null) {
                if (argumentClass != boolean.class && argumentClass != Boolean.class) {
                    throw new RegisterCommandMethodException(method,
                        "The flag annotation is present on a parameter without the arg annonation, however the parameter type is not an boolean.");
                }

                methodArguments.add(flag);

                continue;
            }

            if (flagArgAnnotation == null) {
                CommandArgument argument;
                if (i == methodParameters.length - 1) {
                    //Find the Wildcard annotation
                    Wildcard wildcard = null;
                    for (Annotation annotation : parameterAnnotations[i]) {
                        if (annotation.annotationType() == Wildcard.class) {
                            wildcard = (Wildcard) annotation;
                        }
                    }

                    if (wildcard != null) {
                        boolean join = wildcard.join();
                        if (!join) {
                            argumentClass = argumentClass.getComponentType();
                            if (argumentClass == null) {
                                throw new RegisterCommandMethodException(method,
                                    "The wildcard argument needs to be an array if join is false.");
                            }
                        }
                        this.wildcard = new WildcardArgument(commandArgAnnotation, argumentClass,
                            getArgumenHandler(argumentClass), join);
                        argument = this.wildcard;

                    } else {
                        argument = new CommandArgument(commandArgAnnotation, argumentClass,
                            getArgumenHandler(argumentClass));
                        arguments.add(argument);
                    }
                } else {
                    argument = new CommandArgument(commandArgAnnotation, argumentClass,
                        getArgumenHandler(argumentClass));
                    arguments.add(argument);
                }

                methodArguments.add(argument);
            } else {
                FlagArgument argument = new FlagArgument(commandArgAnnotation, argumentClass,
                    getArgumenHandler(argumentClass), flag);
                methodArguments.add(argument);
                flag.addArgument(argument);
            }
        }

        this.set = true;
    }


//    private String[] addConfigAliases( String label, String[] aliases )
//	{
//    	String[] results = aliases;
//    	
//    	String configKey = "prisonCommandHandler.aliases." + label.replace( " ", "." );
//    	
//    	List<?> ca = Prison.get().getPlatform().getConfigStringArray( configKey );
//    	if ( ca != null && ca.size() > 0 && ca.get( 0 ) instanceof String ) {
//    		
//			List<String> configAliases = new ArrayList<>();
//			
//			for ( String alias : aliases ) {
//				configAliases.add( alias );
//			}
//					
//			for ( Object aliasObj : ca ) {
//				if ( aliasObj instanceof String ) {
//					configAliases.add( aliasObj.toString() );
//				}
//			}
//			
//    		results = configAliases.toArray( new String[0] );
//    		
//    	}
//		return results;
//	}
    
    public boolean testPermission(CommandSender sender) {
        if (!set) {
            return true;
        }

        return handler.getPermissionHandler().hasPermission(sender, permissions);
    }


	@Override
	public int compareTo( RegisteredCommand arg0 )
	{
		return getUsage().compareTo( arg0.getUsage() );
	}
	
    
    public void incrementUsageCount() {
		usageCount++;
	}
    public int getUsageCount() {
    	return usageCount;
    }
	public void setUsageCount(int usageCount) {
		this.usageCount = usageCount;
	}

	public long getUsageRunTimeNanos() {
		return usageRunTimeNanos;
	}
	public void setUsageRunTimeNanos(long usageRunTimeNanos) {
		this.usageRunTimeNanos = usageRunTimeNanos;
	}
	
}
