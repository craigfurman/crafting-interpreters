defmodule CounterTest do
  use ExUnit.Case, async: true
  doctest Counter

  test "counters can count from zero" do
    pid = Counter.start()
    assert Counter.value(pid) == 0
    Counter.increment(pid)
    assert Counter.value(pid) == 1
    Counter.increment(pid)
    Counter.increment(pid)
    assert Counter.value(pid) == 3
    Counter.stop(pid)
  end
end
