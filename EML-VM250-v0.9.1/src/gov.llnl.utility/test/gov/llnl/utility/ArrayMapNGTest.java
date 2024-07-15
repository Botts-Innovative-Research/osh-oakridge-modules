/*
 * Copyright (c) 2021, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-822850
 * 
 * OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION
 * 
 * This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no.  DE-AC52-07NA27344 (Contract 44)
 * between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.
 * See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.
 */
package gov.llnl.utility;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author pham21
 */
public class ArrayMapNGTest
{

  public ArrayMapNGTest()
  {
  }

  /**
   * Test of size method, of class ArrayMap.
   */
  @Test
  public void testSize()
  {
    System.out.println("size");
    ArrayMap<Integer, Integer> instance = new ArrayMap<>()
    {
      {
        put(1, 1);
        put(2, 2);
        put(3, 3);
        put(4, 4);
      }
    };
    assertEquals(instance.size(), 4);
  }

  /**
   * Test of isEmpty method, of class ArrayMap.
   */
  @Test
  public void testIsEmpty()
  {
    System.out.println("isEmpty");
    ArrayMap<Integer, Integer> instance = new ArrayMap<>();
    assertEquals(instance.isEmpty(), true);
    instance.put(1, 1);
    assertEquals(instance.isEmpty(), false);
  }

  /**
   * Test of containsKey method, of class ArrayMap.
   */
  @Test
  public void testContainsKey()
  {
    System.out.println("containsKey");
    ArrayMap<Integer, Integer> instance = new ArrayMap<>()
    {
      {
        put(1, 1);
        put(2, 2);
        put(3, 3);
        put(4, 4);
      }
    };
    assertEquals(instance.containsKey(1), true);
    assertEquals(instance.containsKey(0), false);

    ArrayMap<Double, Integer> doubleMap = new ArrayMap<>()
    {
      {
        put(1.0D, 1);
      }
    };
    assertEquals(doubleMap.containsKey(1.0), true);
    assertEquals(doubleMap.containsKey(0), false);
    assertEquals(doubleMap.containsKey("1"), false);

    ArrayMap<String, Integer> strMap = new ArrayMap<>()
    {
      {
        put("1", 1);
      }
    };
    assertEquals(strMap.containsKey("1"), true);
    assertEquals(strMap.containsKey(1), false);
  }

  /**
   * Test of containsValue method, of class ArrayMap.
   */
  @Test
  public void testContainsValue()
  {
    System.out.println("containsValue");
    ArrayMap<Integer, Integer> instance = new ArrayMap<>()
    {
      {
        put(1, 1);
        put(2, 2);
        put(3, 3);
        put(4, 4);
      }
    };
    ArrayMap<String, Integer> strMap = new ArrayMap<>()
    {
      {
        put("1", 1);
      }
    };

    assertEquals(instance.containsValue(1), true);
    assertEquals(instance.containsValue(0), false);
    assertEquals(instance.containsValue("0"), false);

    assertEquals(strMap.containsValue("1"), false);
    assertEquals(strMap.containsValue(1.0), false);
    assertEquals(strMap.containsValue(1), true);
  }

  /**
   * Test of getEntry method, of class ArrayMap.
   */
  @Test
  public void testGetEntry()
  {
    System.out.println("getEntry");
    Object key = null;
    ArrayMap<Integer, Integer> instance = new ArrayMap<>()
    {
      {
        put(1, 1);
        put(2, 2);
        put(3, 3);
        put(4, 4);
      }
    };
    TestArrayEntry<Integer, Integer> expResult = new TestArrayEntry<>(1, 1);
    Map.Entry result = instance.getEntry(1);
    assertEquals(result.getKey(), expResult.getKey());
    assertEquals(result.getValue(), expResult.getValue());
    assertNull(instance.getEntry("1"));
    assertNull(instance.getEntry(0));
  }

  /**
   * Test of get method, of class ArrayMap.
   */
  @Test
  public void testGet()
  {
    System.out.println("get");
    ArrayMap<Integer, Integer> instance = new ArrayMap<>()
    {
      {
        put(1, 1);
        put(2, 2);
        put(3, 3);
        put(4, 4);
      }
    };
    assertEquals(instance.get(1), Integer.valueOf(1));
    assertNull(instance.get(0));
    assertNull(instance.get("1"));
  }

  /**
   * Test of put method, of class ArrayMap.
   */
  @Test
  public void testPut()
  {
    System.out.println("put");
    ArrayMap<Integer, Integer> instance = new ArrayMap<>();

    Integer key = 1;
    Integer value = 1;
    Integer result = instance.put(key, value);
    assertNull(result);
    assertEquals(instance.put(key, 2), Integer.valueOf(1));

    ArrayMap instance2 = new ArrayMap();
    Object obj = instance2.put(2, "2");
    assertNull(obj);
    obj = instance2.put(3, 3);
    assertNull(obj);
    obj = instance2.put(2, 2);
    assertEquals(obj, "2");
  }

  /**
   * Test of remove method, of class ArrayMap.
   */
  @Test
  public void testRemove()
  {
    System.out.println("remove");
    Object o = null;
    ArrayMap instance = new ArrayMap()
    {
      {
        put(1, 1);
        put("2", 2);
        put(3, "3");
        put("4", "4");
      }
    };
    assertEquals(instance.remove("2"), 2);
    assertNull(instance.remove("2"));
    assertNull(instance.remove(4));
  }

  /**
   * Test of putAll method, of class ArrayMap.
   */
  @Test
  public void testPutAll()
  {
    System.out.println("putAll");
    ArrayMap control = new ArrayMap()
    {
      {
        put(1, 1);
        put("2", 2);
        put(3, "3");
        put("4", "4");
      }
    };
    ArrayMap instance = new ArrayMap();
    instance.putAll(control);

    assertEquals(instance.get(3), "3");
  }

  /**
   * Test of clear method, of class ArrayMap.
   */
  @Test
  public void testClear()
  {
    System.out.println("clear");
    ArrayMap instance = new ArrayMap()
    {
      {
        put(1, 1);
        put("2", 2);
        put(3, "3");
        put("4", "4");
      }
    };
    instance.clear();
    assertEquals(instance.size(), 0);
    assertNull(instance.get(1));
  }

  /**
   * Test of keySet method, of class ArrayMap.
   */
  @Test
  public void testKeySet()
  {
    System.out.println("keySet");
    ArrayMap instance = new ArrayMap()
    {
      {
        put(1, 1);
        put("2", 2);
        put(3, "3");
        put("4", "4");
      }
    };
    Set result = instance.keySet();
    assertTrue(result.contains(1));
    assertTrue(result.contains("2"));
    assertTrue(result.contains(3));
    assertTrue(result.contains("4"));
  }

  /**
   * Test of values method, of class ArrayMap.
   */
  @Test
  public void testValues()
  {
    System.out.println("values");
    Map control = new HashMap()
    {
      {
        put(1, 2);
        put("2", 4);
        put(3, "8");
        put("4", "16");
      }
    };
    ArrayMap instance = new ArrayMap()
    {
      {
        put(1, 2);
        put("2", 4);
        put(3, "8");
        put("4", "16");
      }
    };
    Collection result = instance.values();
    // Collection.contains will not work as intended since we'll be comparing
    // different objects with the same underlaying data.
    Object[] objList = result.toArray();
    assertEquals(objList.length, 4);

    for (Object obj : objList)
    {
      System.out.println(obj);
      assertTrue(instance.containsValue(obj));
      assertTrue(control.containsValue(obj));
    }

    assertEquals(result.size(), control.values().size());

    Map intControl = new HashMap()
    {
      {
        put(1, 2);
        put(2, 4);
        put(3, 8);
        put(4, 16);
      }
    };
    ArrayMap<Integer, Integer> intInstance = new ArrayMap<>()
    {
      {
        put(1, 2);
        put(2, 4);
        put(3, 8);
        put(4, 16);
      }
    };

    Integer[] controlArray = new Integer[intControl.size()];
    controlArray = (Integer[]) intControl.values().toArray(controlArray);

    Integer[] testArray = new Integer[intInstance.size()];
    testArray = (Integer[]) intInstance.values().toArray(testArray);

    assertEquals(testArray.length, controlArray.length);
    for (int i = 0; i < controlArray.length; ++i)
    {
      assertEquals(testArray[i], controlArray[i]);
    }
  }

  /**
   * Test of entrySet method, of class ArrayMap.
   */
  @Test
  public void testEntrySet()
  {
    System.out.println("entrySet");
    ArrayMap instance = new ArrayMap()
    {
      {
        put(1, 2);
        put("2", 4);
        put(3, "8");
        put("4", "16");
      }
    };

    Map control = new HashMap()
    {
      {
        put(1, 2);
        put("2", 4);
        put(3, "8");
        put("4", "16");
      }
    };

    Set<Entry> controlSet = control.entrySet();
    Object[] controlArray = controlSet.toArray();
    Set<Entry> result = instance.entrySet();
    Object[] testArray = result.toArray();

    assertEquals(testArray.length, controlArray.length);
    for (int i = 0; i < controlArray.length; ++i)
    {
      Map.Entry controlEntry = (Map.Entry) controlArray[i];
      Map.Entry testEntry = (Map.Entry) testArray[i];
      assertEquals(controlEntry.getKey(), testEntry.getKey());
      assertEquals(controlEntry.getValue(), testEntry.getValue());
    }
  }

  // Copy class over from ArrayMap.ArrayEntry for testing purposes 
  static class TestArrayEntry<K, V> implements Map.Entry<K, V>, Serializable
  {
    K key;
    V value;

    private TestArrayEntry(K k, V v)
    {
      this.key = k;
      this.value = v;
    }

    @Override
    public K getKey()
    {
      return key;
    }

    @Override
    public V getValue()
    {
      return value;
    }

    @Override
    public V setValue(V v)
    {
      return value;
    }
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