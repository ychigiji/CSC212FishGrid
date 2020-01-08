package edu.smith.cs.csc212.fishgrid;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class manages our model of gameplay: missing and found fish, etc.
 * 
 * @author jfoley
 *
 */

public class FishGame {
	/**
	 * This is the world in which the fish are missing. (It's mostly a List!).
	 */
	World world;
	/**
	 * The player (a Fish.COLORS[0]-colored fish) goes seeking their friends.
	 */
	Fish player;
	
	
	
	/**
	 * Draw the food
	 */
	Food food;
	
	/**
	 * The home location.
	 */
	FishHome home;
	
	
	/**
	 * These are the missing fish!
	 */

	List<Fish> missing;

	/**
	 * This is the snail that is in the world
	 */

	Snail snail;

	/**
	 * These are fish we've found!
	 */
	List<Fish> found;

	/**
	 * These are fish that have returned home!
	 */
	List<Fish> atHome;

	/**
	 * Number of steps!
	 */
	int stepsTaken;

	/**
	 * Score!
	 */
	int score;

	/**
	 * Change the number of rocks here
	 */

	public static final int NUM_ROCKS = 10;

	/**
	 * Create a FishGame of a particular size.
	 * 
	 * @param w how wide is the grid?
	 * @param h how tall is the grid?
	 */
	public FishGame(int w, int h) {
		world = new World(w, h);

		missing = new ArrayList<Fish>();
		found = new ArrayList<Fish>();
		atHome = new ArrayList<Fish>();

		// Add a home!
		home = world.insertFishHome();

		// Generate some more rocks!
		// Make 5 into a constant, so it's easier to find & change.
		for (int i = 0; i < NUM_ROCKS; i++) {
			world.insertRockRandomly();
		}

		// Make 3 snails!
		for (int i = 0; i < 4; i++) {
			snail = world.insertSnailRandomly();
			
		}

		// Insert the falling rocks randomly.

		
			world.insertFallingRockRandomly();
		
		
		// Insert food random
		food = world.insertFoodRandomly();

		// Make the player out of the 0th fish color.
		player = new Fish(0, world);

		// Start the player at "home".
		player.setPosition(home.getX(), home.getY());
		player.markAsPlayer();
		world.register(player);

		// Generate fish of all the colors but the first into the "missing" List.
		for (int ft = 1; ft < Fish.COLORS.length; ft++) {
			Fish friend = world.insertFishRandomly(ft);
			missing.add(friend);
		}
	}

	/**
	 * How we tell if the game is over: if missingFishLeft() == 0.
	 * 
	 * @return the size of the missing list.
	 */
	public int missingFishLeft() {
		return missing.size();
	}

	/**
	 * This method is how the Main app tells whether we're done.
	 * 
	 * @return true if the player has won (or maybe lost?).
	 */
	public boolean gameOver() {
		// bring the fish home before we win!
		if (missing.isEmpty() && found.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Update positions of everything (the user has just pressed a button).
	 */
	public void step() {

		// Keep track of how long the game has run.
		this.stepsTaken += 1;
		
		if (stepsTaken % 5 == 0) {
			world.insertFoodRandomly();
		}

		// Make sure missing fish *do* something.
		wanderMissingFish();
		runFish();
		// These are all the objects in the world in the same cell as the player.
		List<WorldObject> overlap = this.player.findSameCell();
		// The player is there, too, let's skip them.
		overlap.remove(this.player);

		// If we find a fish, remove it from missing.
		for (WorldObject wo : overlap) {
			
			if (wo instanceof Food) {
				world.remove(wo);
				score +=5;
			}
			// It is missing if it's in our missing list.
			if (missing.contains(wo)) {
				// Remove this fish from the missing list.
				missing.remove(wo);
				Fish yolie = (Fish) wo;

				// add to found instead! (So we see objectsFollow work!)
				found.add(yolie);

				if (yolie.getColor() == Color.orange) {
					// Increase score when you find a fish!
					score += 30;
				} else {
					score += 50;
				} // if score
			} // if missing
		} // for loop

		// fish that returns home on their own are marked at home and removed from the
		// world.
		for (Fish lostfish : missing) {
			List<WorldObject> foverlap = lostfish.findSameCell();
			
			for (WorldObject w: foverlap) {
				if (w instanceof Food) {
					world.remove(w);
				}
			}
			if (lostfish.getX() == home.getX() && lostfish.getY() == home.getY()) {
				atHome.add(lostfish);
				world.remove(lostfish);
			}
		}
		// Remove fish that that wandered home
		if (atHome.containsAll(missing)) {
			missing.clear();
		}

		// if the player takes the fish home, it is removed
		// from the world and added to the atHome list

		for (Fish a : found) {
			if (a.getX() == home.getX() && a.getY() == home.getY()) {
				atHome.add(a);

				// Increase score when you find a fish!
				score = score + 50;
				world.remove(a);
			}
		}
		if (atHome.containsAll(found)) {
			found.clear();
		}

		wanderMissingFish();

		// When fish get added to "found" they will follow the player around.
		World.objectsFollow(player, found);
		// Step any world-objects that run themselves.
		world.stepAll();
	}

	/**
	 * Give fish an escape chance
	 */
	public void runFish() {
		// after 20 steps, the fish at the back have 30% chance of getting lost again
		Random rand = ThreadLocalRandom.current();
		if (this.stepsTaken > 20) {
			if (found.size() > 1) {
				Fish fishy = found.get(found.size() - 1);
				if (rand.nextDouble() < 0.99) {
					found.remove(fishy);
					missing.add(fishy);
				}
			}
		}
	}

	/**
	 * Call moveRandomly() on all of the missing fish to make them seem alive.
	 */
	private void wanderMissingFish() {
		Random rand = ThreadLocalRandom.current();
		for (Fish lost : missing) {
			// 30% of the time, lost fish move randomly.
			Fish redd = (Fish) lost;
			if (rand.nextDouble() < .1 || redd.getColor() == Color.red) {
				lost.moveRandomly();

			} else if (rand.nextDouble() < 0.3) {
				lost.moveRandomly();

			}
		}
	}

	/**
	 * This gets a click on the grid. We want it to destroy rocks that ruin the
	 * game.
	 * 
	 * @param x - the x-tile.
	 * @param y - the y-tile.
	 */
	public void click(int x, int y) {
		// TODO(FishGrid) use this print to debug your World.canSwim changes!
		System.out.println("Clicked on: " + x + "," + y + " world.canSwim(player,...)=" + world.canSwim(player, x, y));
		List<WorldObject> atPoint = world.find(x, y);
		for (WorldObject a : atPoint) {
			// Remove rocks
			if (a.isRock()) {
				world.remove(a);
			}
		}

	}

}
