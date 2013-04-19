package com.massivecraft.factions.cmd;

import org.bukkit.Bukkit;

import com.massivecraft.factions.ConfServer;
import com.massivecraft.factions.cmd.arg.ARFaction;
import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.FFlag;
import com.massivecraft.factions.FPerm;
import com.massivecraft.factions.FPlayerColl;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Perm;
import com.massivecraft.factions.integration.SpoutFeatures;
import com.massivecraft.mcore.cmd.req.ReqHasPerm;

public class CmdFactionsDisband extends FCommand
{
	public CmdFactionsDisband()
	{
		this.addAliases("disband");
		
		this.addOptionalArg("faction", "you");
		
		this.addRequirements(ReqHasPerm.get(Perm.DISBAND.node));
	}
	
	@Override
	public void perform()
	{
		Faction faction = this.arg(0, ARFaction.get(), myFaction);
		if (faction == null) return;
		
		if ( ! FPerm.DISBAND.has(sender, faction, true)) return;

		if (faction.getFlag(FFlag.PERMANENT))
		{
			msg("<i>This faction is designated as permanent, so you cannot disband it.");
			return;
		}

		FactionDisbandEvent disbandEvent = new FactionDisbandEvent(me, faction.getId());
		Bukkit.getServer().getPluginManager().callEvent(disbandEvent);
		if(disbandEvent.isCancelled()) return;

		// Send FPlayerLeaveEvent for each player in the faction
		for ( FPlayer fplayer : faction.getFPlayers() )
		{
			Bukkit.getServer().getPluginManager().callEvent(new FPlayerLeaveEvent(fplayer, faction, FPlayerLeaveEvent.PlayerLeaveReason.DISBAND));
		}

		// Inform all players
		for (FPlayer fplayer : FPlayerColl.get().getAllOnline())
		{
			String who = senderIsConsole ? "A server admin" : fme.describeTo(fplayer);
			if (fplayer.getFaction() == faction)
			{
				fplayer.msg("<h>%s<i> disbanded your faction.", who);
			}
			else
			{
				fplayer.msg("<h>%s<i> disbanded the faction %s.", who, faction.getTag(fplayer));
			}
		}
		if (ConfServer.logFactionDisband)
			Factions.get().log("The faction "+faction.getTag()+" ("+faction.getId()+") was disbanded by "+(senderIsConsole ? "console command" : fme.getName())+".");

		if (Econ.isEnabled() && ! senderIsConsole)
		{
			//Give all the faction's money to the disbander
			double amount = Econ.getBalance(faction.getAccountId());
			Econ.transferMoney(fme, faction, fme, amount, false);
			
			if (amount > 0.0)
			{
				String amountString = Econ.moneyString(amount);
				msg("<i>You have been given the disbanded faction's bank, totaling %s.", amountString);
				Factions.get().log(fme.getName() + " has been given bank holdings of "+amountString+" from disbanding "+faction.getTag()+".");
			}
		}		
		
		faction.detach();

		SpoutFeatures.updateTitle(null, null);
		SpoutFeatures.updateCape(null, null);
	}
}
