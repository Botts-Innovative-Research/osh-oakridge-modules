/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility.internal;

import gov.llnl.utility.Tokenizer;
import java.util.regex.Matcher;

/**
 *
 * @author nelson85
 */
public class TokenImpl implements Tokenizer.Token
{
  int offset = 0;
  int index;
  public Matcher matcher;

  /**
   * Gets the id associate with this token.
   *
   * @return the id base on the order of token declarations.
   */
  @Override
  public int id()
  {
    return index;
  }

  /**
   * Returns the start of the token match.
   *
   * @return the index of the first character matched.
   * @throws IllegalStateException if operation failed.
   */
  @Override
  public int start() throws IllegalStateException
  {
    return matcher.start() + offset;
  }

  /**
   * Returns the end of the token match.
   *
   * @return then index of the character past the end of the match.
   * @throws IllegalStateException if operation failed.
   */
  @Override
  public int end() throws IllegalStateException
  {
    return matcher.end() + offset;
  }

  /**
   * Returns the number of groups that were matched with this token.
   *
   * @return the total groups matched.
   * @throws IllegalStateException if operation failed.
   */
  @Override
  public int groupCount() throws IllegalStateException
  {
    return matcher.groupCount();
  }

  /**
   * Returns the offset of the first character matched by a group.
   *
   * @param i is the group number.
   * @return the offset of the first character matched by this group.
   * @throws IllegalStateException if operation failed.
   * @throws IndexOutOfBoundsException if indexing a non-existing group.
   */
  @Override
  public int start(int i) throws IllegalStateException, IndexOutOfBoundsException
  {
    return matcher.start(i) + offset;
  }

  /**
   * Returns the end of the token match by a group.
   *
   * @param i is the group number
   * @return then index of the character past the end of the group.
   * @throws IllegalStateException if operation failed.
   * @throws IndexOutOfBoundsException if indexing a non-existing group.
   */
  @Override
  public int end(int i) throws IllegalStateException, IndexOutOfBoundsException
  {
    return matcher.end(i) + offset;
  }

  /**
   * Get the token that was matched,
   *
   * @return the string that matched this token.
   * @throws IllegalStateException if operation failed.
   */
  @Override
  public String group() throws IllegalStateException
  {
    return matcher.group();
  }

  /**
   * Get the substring that matches the group.
   *
   * @param i
   * @return the string matched by the group.
   * @throws IllegalStateException if operation failed.
   * @throws IndexOutOfBoundsException if indexing a non-existing group.
   */
  @Override
  public String group(int i) throws IllegalStateException, IndexOutOfBoundsException
  {
    return matcher.group(i);
  }

}


/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */